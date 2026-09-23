package com.translator.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 与主应用共用的数据目录：~/.easyReadSkill/config.json */
public final class CredentialPaths {

    /** 构造 CredentialPaths。 */
    private CredentialPaths() {}

    /** dataDir。 */
    public static Path dataDir() {
        return Paths.get(System.getProperty("user.home"), ".easyReadSkill");
    }

    /** configFile。 */
    public static Path configFile() {
        return dataDir().resolve("config.json");
    }
}
