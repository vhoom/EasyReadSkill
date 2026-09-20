package com.song.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private static final Path DATA_DIR =
            Paths.get(System.getProperty("user.home"), ".easyReadSkill");
    private static final Path CONFIG_FILE = DATA_DIR.resolve("config.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    public static Path getDataDir() { return DATA_DIR; }
    public static Path getConfigFile() { return CONFIG_FILE; }

    public static void ensureDataDir() throws IOException {
        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
    }

    public static AppConfig load() {
        try {
            ensureDataDir();
            if (!Files.exists(CONFIG_FILE)) {
                AppConfig config = new AppConfig();
                save(config);
                return config;
            }
            String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            AppConfig config = GSON.fromJson(json, AppConfig.class);
            if (config == null) config = new AppConfig();
            config.migrateLegacy();
            config.ensureDefaultScanPaths();
            return config;
        } catch (IOException e) {
            LOG.error("加载配置失败: {}", CONFIG_FILE, e);
            return new AppConfig();
        }
    }

    public static void save(AppConfig config) {
        try {
            ensureDataDir();
            String json = GSON.toJson(config);
            Path tmp = CONFIG_FILE.resolveSibling("config.json.tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, CONFIG_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("保存配置失败: {}", CONFIG_FILE, e);
        }
    }
}