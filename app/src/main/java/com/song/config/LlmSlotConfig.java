package com.song.config;

import com.llm.api.LlmPrompts;
import com.llm.api.LlmVendor;

/**
 * 单个 OpenAI 兼容大模型厂商的持久化配置。
 *
 */
public class LlmSlotConfig {

    private String apiKey = "";
    private String baseUrl = "";
    private String model = "";
    /** 不写入 config.json */
    private transient String prompt = LlmPrompts.DEFAULT_TRANSLATE_SYSTEM;

    public String getApiKey() { return apiKey == null ? "" : apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey; }

    public String getBaseUrl() { return baseUrl == null ? "" : baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl == null ? "" : baseUrl; }

    public String getModel() { return model == null ? "" : model; }
    public void setModel(String model) { this.model = model == null ? "" : model; }

    public void resetPromptToDefault() {
        this.prompt = LlmPrompts.DEFAULT_TRANSLATE_SYSTEM;
    }

    public String getPrompt() {
        return prompt == null || prompt.isBlank()
                ? LlmPrompts.DEFAULT_TRANSLATE_SYSTEM : prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 解析生效 Base URL：配置优先，否则厂商默认。
     *
     * @param vendor 厂商
     * @return Base URL
     */
    public String resolveBaseUrl(LlmVendor vendor) {
        String v = getBaseUrl();
        return v.isBlank() ? vendor.defaultBaseUrl() : v.trim();
    }

    /**
     * 解析生效模型：配置优先，否则厂商默认。
     *
     * @param vendor 厂商
     * @return 模型 id
     */
    public String resolveModel(LlmVendor vendor) {
        String v = getModel();
        return v.isBlank() ? vendor.defaultModel() : v.trim();
    }
}
