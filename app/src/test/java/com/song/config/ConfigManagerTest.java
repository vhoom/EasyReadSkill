package com.song.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @TempDir
    Path temp;

    @Test
    void freshConfigGetsDefaultsAndVersion() throws Exception {
        Path file = temp.resolve("config.json");
        AppConfig config = ConfigManager.load(file);

        assertEquals(AppConfig.SCHEMA_VERSION, config.getSchemaVersion());
        assertFalse(config.getScanPaths().isEmpty());
        assertTrue(Files.exists(file));
    }

    @Test
    void legacyConfigIsBackedUpBeforeMigration() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file,
                "{\"vendor\":\"YOUDAO\",\"scanPaths\":[{\"path\":\"/x\",\"enabled\":true}]}",
                StandardCharsets.UTF_8);

        AppConfig config = ConfigManager.load(file);

        assertEquals(AppConfig.SCHEMA_VERSION, config.getSchemaVersion());
        assertTrue(Files.exists(temp.resolve("config.json.legacy.bak")));
        assertEquals(1, config.getScanPaths().size());
        assertEquals("/x", config.getScanPaths().get(0).getPath());
    }

    @Test
    void emptiedScanPathsAreNotRefilled() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, "{\"schemaVersion\":2,\"scanPaths\":[]}",
                StandardCharsets.UTF_8);

        assertTrue(ConfigManager.load(file).getScanPaths().isEmpty());
    }

    @Test
    void blankValuesDoNotWipeKeysOnDisk() throws Exception {
        Path file = temp.resolve("config.json");
        AppConfig writer = new AppConfig();
        writer.setSchemaVersion(AppConfig.SCHEMA_VERSION);
        writer.getBaiduConfig().setAppId("disk-id");
        writer.getBaiduConfig().setSecretKey("disk-secret");
        writer.getLlmConfigs().getDeepseek().setApiKey("sk-disk");
        ConfigManager.save(file, writer);

        // 另一个实例内存里是空的，保存时不能把磁盘上的密钥清掉
        AppConfig empty = new AppConfig();
        empty.setSchemaVersion(AppConfig.SCHEMA_VERSION);
        ConfigManager.save(file, empty);

        String json = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(json.contains("disk-id"));
        assertTrue(json.contains("disk-secret"));
        assertTrue(json.contains("sk-disk"));
    }

    @Test
    void baseUrlAndModelCanStillBeCleared() throws Exception {
        Path file = temp.resolve("config.json");
        AppConfig writer = new AppConfig();
        writer.setSchemaVersion(AppConfig.SCHEMA_VERSION);
        writer.getLlmConfigs().getDeepseek().setBaseUrl("https://old.example");
        writer.getLlmConfigs().getDeepseek().setModel("old-model");
        ConfigManager.save(file, writer);

        AppConfig cleared = new AppConfig();
        cleared.setSchemaVersion(AppConfig.SCHEMA_VERSION);
        ConfigManager.save(file, cleared);

        AppConfig reloaded = ConfigManager.load(file);
        assertEquals("", reloaded.getLlmConfigs().getDeepseek().getBaseUrl());
        assertEquals("", reloaded.getLlmConfigs().getDeepseek().getModel());
    }

    @Test
    void unknownTopLevelFieldsSurviveSave() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file,
                "{\"schemaVersion\":2,\"writtenBySomethingElse\":{\"a\":1}}",
                StandardCharsets.UTF_8);

        AppConfig config = ConfigManager.load(file);
        ConfigManager.save(file, config);

        assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                .contains("writtenBySomethingElse"));
    }

    @Test
    void brokenJsonIsQuarantined() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        AppConfig config = ConfigManager.load(file);

        assertNotNull(config);
        assertTrue(Files.exists(temp.resolve("config.json.bad")));
    }
}
