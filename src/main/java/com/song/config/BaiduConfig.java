package com.song.config;

/**
 * 百度翻译配置。
 */
public class BaiduConfig {
    private String appId = "";
    private String apiKey = "";
    private String secretKey = "";
    private String domain = "it";

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSecretKey() {
        return secretKey != null && !secretKey.isEmpty() ? secretKey : apiKey;
    }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getDomain() {
        return domain != null && !domain.isEmpty() ? domain : "it";
    }
    public void setDomain(String domain) { this.domain = domain; }
}