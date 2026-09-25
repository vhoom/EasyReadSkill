package com.translator.config;

import com.translator.baidu.BaiduConfig;
import com.translator.youdao.YoudaoConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialStoreTest {

    @TempDir
    Path temp;

    @Test
    void mapsNestedSectionsToRuntimeConfigs() {
        StoredCredentials c = new StoredCredentials();
        c.getYoudao().setAppId("yk");
        c.getYoudao().setSecretKey("ys");
        c.getBaidu().setAppId("bid");
        c.getBaidu().setSecretKey("bsec");

        YoudaoConfig y = c.toYoudaoConfigOrNull();
        BaiduConfig b = c.toBaiduConfigOrNull();
        assertNotNull(y);
        assertNotNull(b);
        assertEquals("yk", y.appKey());
        assertEquals("ys", y.appSecret());
        assertEquals("bid", b.appId());
        assertEquals("bsec", b.secret());
    }

    @Test
    void baiduFallsBackToApiKey() {
        StoredCredentials c = new StoredCredentials();
        c.getBaidu().setAppId("bid");
        c.getBaidu().setApiKey("fromApi");
        assertEquals("fromApi", StoredCredentials.resolveBaiduSecret(c.getBaidu()));
        assertNotNull(c.toBaiduConfigOrNull());
    }

    @Test
    void migrateLegacyFlatFields() {
        StoredCredentials c = new StoredCredentials();
        // 通过 JSON 模拟旧扁平字段
        c = new com.google.gson.Gson().fromJson(
                "{\"appId\":\"oldId\",\"secretKey\":\"oldSec\",\"domain\":\"finance\","
                        + "\"youdaoDomain\":\"medicine\",\"prompt\":\"学术风格\"}",
                StoredCredentials.class);
        c.migrateLegacy();
        assertEquals("oldId", c.getBaidu().getAppId());
        assertEquals("oldSec", c.getBaidu().getSecretKey());
        assertEquals("finance", c.getBaidu().getDomain());
        assertEquals("oldId", c.getYoudao().getAppId());
        assertEquals("oldSec", c.getYoudao().getSecretKey());
        assertEquals("medicine", c.getYoudao().getDomain());
        assertEquals("学术风格", c.getYoudao().getPrompt());
    }

    @Test
    void incompleteKeysYieldNull() {
        StoredCredentials c = new StoredCredentials();
        c.getYoudao().setAppId("onlyKey");
        assertNull(c.toYoudaoConfigOrNull());
        assertNull(c.toBaiduConfigOrNull());
    }

    @Test
    void saveMergesWithoutWipingOtherFields() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file,
                "{\"scanPaths\":[{\"path\":\"/tmp\",\"enabled\":true}],\"baidu\":{\"appId\":\"x\"}}",
                StandardCharsets.UTF_8);

        StoredCredentials creds = new StoredCredentials();
        creds.getBaidu().setAppId("newId");
        creds.getBaidu().setSecretKey("newSec");
        creds.getYoudao().setAppId("yId");
        creds.getYoudao().setSecretKey("ySec");
        CredentialStore.save(file, creds);

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("scanPaths"));
        assertTrue(after.contains("newId"));
        assertTrue(after.contains("yId"));

        StoredCredentials reloaded = new com.google.gson.Gson().fromJson(after, StoredCredentials.class);
        reloaded.migrateLegacy();
        assertEquals("newId", reloaded.getBaidu().getAppId());
        assertEquals("yId", reloaded.getYoudao().getAppId());
    }

    @Test
    void blankValuesDoNotWipeExistingSecrets() throws Exception {
        Path file = temp.resolve("config.json");
        Files.writeString(file,
                "{\"baidu\":{\"appId\":\"keepId\",\"secretKey\":\"keepSec\"},"
                        + "\"youdao\":{\"appId\":\"yId\",\"secretKey\":\"ySec\"}}",
                StandardCharsets.UTF_8);

        StoredCredentials empty = new StoredCredentials();
        CredentialStore.save(file, empty);

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("keepId"));
        assertTrue(after.contains("keepSec"));
        assertTrue(after.contains("ySec"));
    }
}
