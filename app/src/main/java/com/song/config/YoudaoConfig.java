package com.song.config;

/**
 * 有道智云配置。
 *
 */
public class YoudaoConfig {

    /** LLM 系统默认提示词（不持久化）。 */
    public static final String DEFAULT_LLM_PROMPT =
            "你是名翻译全球语言翻译专家，精通各种国家文化习俗思维地道方言俚语和表达习惯，"
                    + "并且能准确的翻译成其它国家的语言且符合当地人们的表达思维习惯，"
                    + "可适当调整语序逻辑但仍要保持与含义原文一致。"
                    + "对于特殊的句子字符串不应该强行翻译如代码、变量、命令、突然创建命名出的一个直译没意义的名词。";

    private String appId = "";
    private String secretKey = "";
    private String domain = "general";
    private String handleOption = "deepseek-flash";
    /** 不写入 config.json */
    private transient String prompt = DEFAULT_LLM_PROMPT;

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

    public void resetPromptToDefault() {
        this.prompt = DEFAULT_LLM_PROMPT;
    }

    public boolean isUsingDefaultPrompt() {
        String current = getPrompt();
        return DEFAULT_LLM_PROMPT.equals(current);
    }

    public String getPrompt() {
        return prompt == null || prompt.isBlank() ? DEFAULT_LLM_PROMPT : prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
