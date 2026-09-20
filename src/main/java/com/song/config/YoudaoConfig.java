package com.song.config;

/**
 * 有道智云配置。
 */
public class YoudaoConfig {
    private String appId = "";
    private String secretKey = "";
    private String domain = "general";
    private String handleOption = "deepseek-flash";
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
        if ("0".equals(handleOption)) return "deepseek-flash";
        if ("3".equals(handleOption)) return "deepseek-v4-flash";
        return handleOption != null ? handleOption : "deepseek-flash";
    }
    public void setHandleOption(String handleOption) { this.handleOption = handleOption; }

    public String getPrompt() {
        return prompt != null ? prompt : "";
    }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}