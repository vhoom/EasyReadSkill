package com.song.config;

/**
 * 百度翻译配置。
 * 提示词仅会话内有效，不落盘；每次启动重置为系统默认。
 */
public class BaiduConfig {
    private String appId = "";
    private String apiKey = "";
    private String secretKey = "";
    private String domain = "it";
    /** 不写入 config.json */
    private transient String prompt = YoudaoConfig.DEFAULT_LLM_PROMPT;

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

    public void resetPromptToDefault() {
        this.prompt = YoudaoConfig.DEFAULT_LLM_PROMPT;
    }

    public boolean isUsingDefaultPrompt() {
        return YoudaoConfig.DEFAULT_LLM_PROMPT.equals(getPrompt());
    }

    public String getPrompt() {
        return prompt == null || prompt.isBlank()
                ? YoudaoConfig.DEFAULT_LLM_PROMPT : prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
