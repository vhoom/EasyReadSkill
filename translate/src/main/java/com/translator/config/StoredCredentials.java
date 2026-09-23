package com.translator.config;

import com.translator.baidu.BaiduConfig;
import com.translator.youdao.YoudaoConfig;

/**
 * 与主包 {@code ~/.easyReadSkill/config.json} 中 baidu / youdao 段对齐的密钥视图。
 * 供 Gson 反序列化；也可由 CredentialStore 手工填充。
 */
public final class StoredCredentials {

    private BaiduSection baidu = new BaiduSection();
    private YoudaoSection youdao = new YoudaoSection();

    /** 旧版扁平字段，加载后迁入 baidu/youdao */
    private String appId;
    private String apiKey;
    private String secretKey;
    private String domain;
    private String youdaoDomain;
    private String handleOption;
    private String prompt;

    /** 获取Baidu。 */
    public BaiduSection getBaidu() {
        if (baidu == null) {
            baidu = new BaiduSection();
        }
        return baidu;
    }

    /** 设置Baidu。 */
    public void setBaidu(BaiduSection baidu) {
        this.baidu = baidu;
    }

    /** 获取Youdao。 */
    public YoudaoSection getYoudao() {
        if (youdao == null) {
            youdao = new YoudaoSection();
        }
        return youdao;
    }

    /** 设置Youdao。 */
    public void setYoudao(YoudaoSection youdao) {
        this.youdao = youdao;
    }

    /** migrateLegacy。 */
    public void migrateLegacy() {
        BaiduSection b = getBaidu();
        YoudaoSection y = getYoudao();

        if (isBlank(b.appId)) {
            b.appId = appId;
        }
        if (isBlank(b.apiKey)) {
            b.apiKey = apiKey;
        }
        if (isBlank(b.secretKey)) {
            b.secretKey = secretKey;
        }
        if (isBlank(b.domain)) {
            b.domain = domain;
        }

        if (isBlank(y.appId)) {
            y.appId = appId;
        }
        if (isBlank(y.secretKey)) {
            y.secretKey = secretKey;
        }
        if (isBlank(y.domain)) {
            y.domain = youdaoDomain;
        }
        if (isBlank(y.handleOption)) {
            y.handleOption = handleOption;
        }
        if (isBlank(y.prompt)) {
            y.prompt = prompt;
        }

        appId = null;
        apiKey = null;
        secretKey = null;
        domain = null;
        youdaoDomain = null;
        handleOption = null;
        prompt = null;
    }

    /** 密钥齐全时返回运行时配置，否则 null */
    public YoudaoConfig toYoudaoConfigOrNull() {
        YoudaoSection y = getYoudao();
        if (isBlank(y.appId) || isBlank(y.secretKey)) {
            return null;
        }
        return new YoudaoConfig(y.appId.trim(), y.secretKey.trim());
    }

    /** 密钥齐全时返回运行时配置，否则 null（secret 兼容 apiKey） */
    public BaiduConfig toBaiduConfigOrNull() {
        BaiduSection b = getBaidu();
        String secret = resolveBaiduSecret(b);
        if (isBlank(b.appId) || isBlank(secret)) {
            return null;
        }
        return new BaiduConfig(b.appId.trim(), secret.trim());
    }

    /** baiduDomainOrEmpty。 */
    public String baiduDomainOrEmpty() {
        BaiduSection b = getBaidu();
        return b.domain != null ? b.domain : "";
    }

    /** youdaoPromptOrEmpty。 */
    public String youdaoPromptOrEmpty() {
        YoudaoSection y = getYoudao();
        return y.prompt != null ? y.prompt : "";
    }

    /** resolveBaiduSecret。 */
    public static String resolveBaiduSecret(BaiduSection b) {
        if (b == null) {
            return "";
        }
        if (!isBlank(b.secretKey)) {
            return b.secretKey;
        }
        return b.apiKey != null ? b.apiKey : "";
    }

    /** 是否Blank。 */
    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class BaiduSection {
        String appId = "";
        String apiKey = "";
        String secretKey = "";
        String domain = "";

        /** 获取AppId。 */
        public String getAppId() { return appId; }
        /** 设置AppId。 */
        public void setAppId(String appId) { this.appId = appId; }
        /** 获取ApiKey。 */
        public String getApiKey() { return apiKey; }
        /** 设置ApiKey。 */
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        /** 获取SecretKey。 */
        public String getSecretKey() { return secretKey; }
        /** 设置SecretKey。 */
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        /** 获取Domain。 */
        public String getDomain() { return domain; }
        /** 设置Domain。 */
        public void setDomain(String domain) { this.domain = domain; }
    }

    public static final class YoudaoSection {
        String appId = "";
        String secretKey = "";
        String domain = "";
        String handleOption = "";
        String prompt = "";

        /** 获取AppId。 */
        public String getAppId() { return appId; }
        /** 设置AppId。 */
        public void setAppId(String appId) { this.appId = appId; }
        /** 获取SecretKey。 */
        public String getSecretKey() { return secretKey; }
        /** 设置SecretKey。 */
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        /** 获取Domain。 */
        public String getDomain() { return domain; }
        /** 设置Domain。 */
        public void setDomain(String domain) { this.domain = domain; }
        /** 获取HandleOption。 */
        public String getHandleOption() { return handleOption; }
        /** 设置HandleOption。 */
        public void setHandleOption(String handleOption) { this.handleOption = handleOption; }
        /** 获取Prompt。 */
        public String getPrompt() { return prompt; }
        /** 设置Prompt。 */
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }
}
