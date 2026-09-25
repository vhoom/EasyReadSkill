package com.song.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;

/**
 * config.json 读写（默认 {@code ~/.easyReadSkill/config.json}）。
 *
 * <p>写入策略，避免"数据存储"类问题：</p>
 * <ul>
 *   <li>只覆盖本应用认识的顶层字段，磁盘上其它字段原样保留</li>
 *   <li>本进程没动过密钥时采用磁盘上的最新密钥（app 与 translate 模块两个写入者互不覆盖）</li>
 *   <li>空白不覆盖磁盘上已有的非空值（只有 baseUrl / model 允许被清空，表示"用厂商默认"）</li>
 *   <li>结构版本升级前留一份 {@code .v<旧版本>.bak}，解析失败的文件留一份 {@code .bad}</li>
 * </ul>
 */
public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private static final Path DATA_DIR =
            Paths.get(System.getProperty("user.home"), ".easyReadSkill");
    private static final Path CONFIG_FILE = DATA_DIR.resolve("config.json");
    private static final String CONFIG_NAME = "config.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    /** 含连接设置的顶层段。 */
    private static final String[] SECTIONS = {"baidu", "youdao", "llm"};

    /** 允许用户清空的字段（清空即"用厂商默认"）。其余字段空白时不覆盖磁盘上的值。 */
    private static final Set<String> CLEARABLE = Set.of("baseUrl", "model");

    public static Path getDataDir() { return DATA_DIR; }
    public static Path getConfigFile() { return CONFIG_FILE; }

    public static void ensureDataDir() throws IOException {
        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
    }

    public static AppConfig load() {
        return load(CONFIG_FILE);
    }

    /**
     * 从指定文件加载（测试或自定义位置）。
     *
     * @param file 配置文件
     * @return 配置；读不到时返回默认配置
     */
    public static AppConfig load(Path file) {
        try {
            Path dir = file.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            if (!Files.exists(file)) {
                AppConfig fresh = new AppConfig();
                fresh.setSchemaVersion(AppConfig.SCHEMA_VERSION);
                save(file, fresh);
                return fresh;
            }

            String json = Files.readString(file, StandardCharsets.UTF_8);
            AppConfig config;
            try {
                config = GSON.fromJson(json, AppConfig.class);
            } catch (RuntimeException e) {
                LOG.error("配置 JSON 损坏，已留现场: {}", file, e);
                quarantine(file);
                config = new AppConfig();
            }
            if (config == null) {
                config = new AppConfig();
            }
            config.migrateLegacy();

            Integer version = config.getSchemaVersion();
            if (version == null || version < AppConfig.SCHEMA_VERSION) {
                backupOnce(file, version == null ? "legacy" : "v" + version);
                config.setSchemaVersion(AppConfig.SCHEMA_VERSION);
                save(file, config);
            }

            config.ensureDefaultScanPaths();
            config.markSecretsLoaded();
            return config;
        } catch (IOException e) {
            LOG.error("加载配置失败: {}", file, e);
            return new AppConfig();
        }
    }

    public static void save(AppConfig config) {
        save(CONFIG_FILE, config);
    }

    /**
     * 写入配置：合并磁盘上本应用不认识的字段，并保护密钥不被空白覆盖。
     *
     * @param file   配置文件
     * @param config 配置
     */
    public static void save(Path file, AppConfig config) {
        if (config == null) {
            return;
        }
        try {
            if (config.getSchemaVersion() == null) {
                config.setSchemaVersion(AppConfig.SCHEMA_VERSION);
            }
            Path dir = file.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }

            JsonObject tree = GSON.toJsonTree(config).getAsJsonObject();
            JsonObject disk = readTree(file);
            if (disk != null) {
                for (Map.Entry<String, JsonElement> entry : disk.entrySet()) {
                    if (!tree.has(entry.getKey())) {
                        tree.add(entry.getKey(), entry.getValue());
                    }
                }
                if (config.secretsChangedSinceLoad()) {
                    adoptNonBlankSecrets(tree, disk);
                } else {
                    adoptDiskSecrets(tree, disk);
                }
            }
            writeAtomically(file, GSON.toJson(tree));
            config.markSecretsLoaded();
        } catch (IOException e) {
            LOG.error("保存配置失败: {}", file, e);
        }
    }

    /**
     * 一次性备份：目标已存在则跳过（幂等）。
     *
     * @param file 源文件
     * @param tag  标记，如 v1 / legacy
     * @return 是否已有备份
     */
    public static boolean backupOnce(Path file, String tag) {
        if (file == null || tag == null || !Files.exists(file)) {
            return false;
        }
        Path backup = file.resolveSibling(file.getFileName() + "." + tag + ".bak");
        if (Files.exists(backup)) {
            return true;
        }
        try {
            Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            LOG.info("已备份 {} -> {}", file, backup);
            return true;
        } catch (IOException e) {
            LOG.warn("备份失败 {}: {}", file, e.toString());
            return false;
        }
    }

    /** 坏文件留现场，避免下次启动继续用坏数据。 */
    private static void quarantine(Path file) {
        try {
            Files.move(file, file.resolveSibling(CONFIG_NAME + ".bad"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.warn("备份损坏配置失败: {}", e.toString());
        }
    }

    private static JsonObject readTree(Path file) {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            JsonElement parsed = com.google.gson.JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            LOG.debug("读取现有配置失败，按空配置写入: {}", e.toString());
            return null;
        }
    }

    private static void writeAtomically(Path file, String json) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 本进程没动过密钥：直接用磁盘上的段。 */
    private static void adoptDiskSecrets(JsonObject memory, JsonObject disk) {
        for (String section : SECTIONS) {
            JsonElement value = disk.get(section);
            if (value != null && value.isJsonObject()) {
                memory.add(section, value);
            }
        }
    }

    /** 本进程改过密钥：只让磁盘上的非空密钥填进空位。 */
    private static void adoptNonBlankSecrets(JsonObject memory, JsonObject disk) {
        for (String section : SECTIONS) {
            JsonElement value = disk.get(section);
            if (value != null && value.isJsonObject()) {
                mergeNonBlankSecrets(memory, section, value.getAsJsonObject());
            }
        }
    }

    private static void mergeNonBlankSecrets(JsonObject parent, String key, JsonObject diskSection) {
        JsonElement current = parent.get(key);
        JsonObject memorySection = current != null && current.isJsonObject()
                ? current.getAsJsonObject() : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : diskSection.entrySet()) {
            String field = entry.getKey();
            JsonElement diskValue = entry.getValue();
            if (diskValue != null && diskValue.isJsonObject()) {
                mergeNonBlankSecrets(memorySection, field, diskValue.getAsJsonObject());
                continue;
            }
            if (CLEARABLE.contains(field)) {
                continue;
            }
            JsonElement memoryValue = memorySection.get(field);
            if (isBlank(memoryValue) && !isBlank(diskValue)) {
                memorySection.add(field, diskValue);
            }
        }
        parent.add(key, memorySection);
    }

    private static boolean isBlank(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return true;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString().isBlank();
        }
        return false;
    }
}
