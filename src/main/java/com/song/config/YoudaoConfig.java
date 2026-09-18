package com.song.config;

/**
 * 有道智云配置。
 */
public class YoudaoConfig {
    private String appId = "";
    private String secretKey = "";
    private String domain = "general";
    private String handleOption = "0";
    private String prompt = "";

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getDomain() {
        return domain != null && !domain.isEmpty() ? domain : "general";
    }
    public void setDomain(String domain) { this.domain = domain; }

    public String getHandleOption() {
        return handleOption != null ? handleOption : "0";
    }
    public void setHandleOption(String handleOption) { this.handleOption = handleOption; }

    public String getPrompt() {
        return prompt != null ? prompt : "";
    }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}