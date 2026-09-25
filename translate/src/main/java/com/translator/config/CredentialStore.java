package com.translator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.translator.config.StoredCredentials.BaiduSection;
import com.translator.config.StoredCredentials.YoudaoSection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * 读写主应用共用的 {@code ~/.easyReadSkill/config.json} 中的翻译密钥。
 * 保存时只改 baidu / youdao 段，保留扫描路径等其它字段。
 */
public final class CredentialStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /** 构造 CredentialStore。 */
    private CredentialStore() {}

    /** load。 */
    public static StoredCredentials load() {
        Path file = CredentialPaths.configFile();
        try {
            if (!Files.exists(file)) {
                return new StoredCredentials();
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            StoredCredentials creds = GSON.fromJson(json, StoredCredentials.class);
            if (creds == null) {
                creds = new StoredCredentials();
            }
            creds.migrateLegacy();
            return creds;
        } catch (Exception e) {
            return new StoredCredentials();
        }
    }

    /**
     * 将当前有道/百度密钥写回 config.json。
     * 文件不存在时创建仅含密钥段的新文件；已存在则合并，不覆盖其它配置。
     */
    public static void save(StoredCredentials creds) throws IOException {
        save(CredentialPaths.configFile(), creds);
    }

    /** 写入指定路径（测试或自定义位置） */
    public static void save(Path file, StoredCredentials creds) throws IOException {
        if (creds == null) {
            throw new IllegalArgumentException("creds 不能为 null");
        }
        Path dir = file.getParent();
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        JsonObject root;
        if (Files.exists(file)) {
            String existing = Files.readString(file, StandardCharsets.UTF_8);
            root = JsonParser.parseString(existing).getAsJsonObject();
        } else {
            root = new JsonObject();
        }

        // 字段级合并：空白值不覆盖磁盘上已有的非空密钥，避免两个写入者互相清空
        root.add("baidu", mergeSection(section(root, "baidu"),
                GSON.toJsonTree(creds.getBaidu()),
                "appId", "apiKey", "secretKey", "domain"));
        root.add("youdao", mergeSection(section(root, "youdao"),
                GSON.toJsonTree(creds.getYoudao()),
                "appId", "secretKey", "domain", "handleOption", "prompt"));

        // 清掉已迁移的旧扁平字段，避免下次再覆盖
        root.remove("appId");
        root.remove("apiKey");
        root.remove("secretKey");
        root.remove("domain");
        root.remove("youdaoDomain");
        root.remove("handleOption");
        root.remove("prompt");

        String json = GSON.toJson(root);
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 把新值合并进旧段：新值为空白而旧值非空时保留旧值。
     *
     * @param oldSection 磁盘上的旧段，可为 null
     * @param newTree    待写入的新段
     * @param fields     需要按此规则合并的字段名
     * @return 合并后的段
     */
    static JsonObject mergeSection(JsonObject oldSection, JsonElement newTree, String... fields) {
        JsonObject next = newTree != null && newTree.isJsonObject()
                ? newTree.getAsJsonObject().deepCopy()
                : new JsonObject();
        if (oldSection == null) {
            return next;
        }
        for (String field : fields) {
            String incoming = next.has(field) && !next.get(field).isJsonNull()
                    ? next.get(field).getAsString()
                    : "";
            if (!incoming.isBlank()) {
                continue;
            }
            if (oldSection.has(field) && !oldSection.get(field).isJsonNull()
                    && !oldSection.get(field).getAsString().isBlank()) {
                next.add(field, oldSection.get(field));
            }
        }
        return next;
    }

    private static JsonObject section(JsonObject root, String name) {
        if (root == null || !root.has(name) || !root.get(name).isJsonObject()) {
            return null;
        }
        return root.getAsJsonObject(name);
    }

    /** 用 UI 当前值更新内存中的 baidu/youdao 段（不写盘） */
    public static void applyUiKeys(StoredCredentials creds,
                                   String youdaoAppId, String youdaoSecret,
                                   String baiduAppId, String baiduSecret) {
        YoudaoSection y = creds.getYoudao();
        if (youdaoAppId != null) {
            y.setAppId(youdaoAppId);
        }
        if (youdaoSecret != null) {
            y.setSecretKey(youdaoSecret);
        }
        BaiduSection b = creds.getBaidu();
        if (baiduAppId != null) {
            b.setAppId(baiduAppId);
        }
        if (baiduSecret != null) {
            b.setSecretKey(baiduSecret);
        }
    }
}
