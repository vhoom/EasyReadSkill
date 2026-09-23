package com.llm.api;

/**
 * OpenAI 兼容厂商。
 * 环境变量优先读 {@link #apiKeyEnvs()}[0]，缺则依次回退。
 * {@link #completionsPath()} / {@link #modelsPath()} 供 OpenAI 兼容 HTTP 客户端使用。
 */
public enum LlmVendor {

    /** DeepSeek。 */
    DEEPSEEK(
            "DeepSeek",
            "https://api.deepseek.com",
            "deepseek-flash",
            "/v1/chat/completions",
            "/v1/models",
            new String[] {"DEEPSEEK_API_KEY"},
            "DEEPSEEK_BASE_URL",
            "DEEPSEEK_MODEL"),

    /** 智谱 GLM。 */
    ZHIPU(
            "智谱 GLM",
            "https://open.bigmodel.cn/api/paas/v4",
            "glm-4-flash",
            "/chat/completions",
            "/models",
            new String[] {"ZHIPU_API_KEY", "BIGMODEL_API_KEY"},
            "ZHIPU_BASE_URL",
            "ZHIPU_MODEL"),

    /** Google Gemini（OpenAI 兼容层）。 */
    GEMINI(
            "Gemini",
            "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini-2.5-flash",
            "/chat/completions",
            "/models",
            new String[] {"GEMINI_API_KEY"},
            "GEMINI_BASE_URL",
            "GEMINI_MODEL"),

    /** Moonshot Kimi。 */
    KIMI(
            "Kimi",
            "https://api.moonshot.cn/v1",
            "kimi-k2-0905-preview",
            "/chat/completions",
            "/models",
            new String[] {"KIMI_API_KEY", "MOONSHOT_API_KEY"},
            "KIMI_BASE_URL",
            "KIMI_MODEL"),

    /** OpenAI（Base 不含 /v1，由 path 带上）。 */
    OPENAI(
            "OpenAI",
            "https://api.openai.com",
            "gpt-4o-mini",
            "/v1/chat/completions",
            "/v1/models",
            new String[] {"OPENAI_API_KEY"},
            "OPENAI_BASE_URL",
            "OPENAI_MODEL"),

    /** 通义千问（DashScope 兼容模式）。 */
    QIANWEN(
            "通义千问",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "qwen-plus",
            "/chat/completions",
            "/models",
            new String[] {"DASHSCOPE_API_KEY", "QIANWEN_API_KEY"},
            "DASHSCOPE_BASE_URL",
            "QIANWEN_MODEL");

    private final String displayName;
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final String completionsPath;
    private final String modelsPath;
    private final String[] apiKeyEnvs;
    private final String baseUrlEnv;
    private final String modelEnv;

    /**
     * @param displayName      展示名
     * @param defaultBaseUrl   默认 Base URL（无尾斜杠）
     * @param defaultModel     默认模型 id
     * @param completionsPath  Chat Completions 路径（相对 base）
     * @param modelsPath       Models 列表路径（相对 base）
     * @param apiKeyEnvs       API Key 环境变量候选（按序）
     * @param baseUrlEnv       覆盖 Base URL 的环境变量名
     * @param modelEnv         覆盖默认模型的环境变量名
     */
    LlmVendor(String displayName, String defaultBaseUrl, String defaultModel,
              String completionsPath, String modelsPath,
              String[] apiKeyEnvs, String baseUrlEnv, String modelEnv) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.completionsPath = completionsPath;
        this.modelsPath = modelsPath;
        this.apiKeyEnvs = apiKeyEnvs;
        this.baseUrlEnv = baseUrlEnv;
        this.modelEnv = modelEnv;
    }

    /** @return 展示名 */
    public String displayName() { return displayName; }

    /** @return 默认 Base URL */
    public String defaultBaseUrl() { return defaultBaseUrl; }

    /** @return 默认模型 id */
    public String defaultModel() { return defaultModel; }

    /** @return Chat Completions 相对路径 */
    public String completionsPath() { return completionsPath; }

    /** @return Models 相对路径 */
    public String modelsPath() { return modelsPath; }

    /** @return API Key 环境变量名副本 */
    public String[] apiKeyEnvs() { return apiKeyEnvs.clone(); }

    /** @return Base URL 环境变量名 */
    public String baseUrlEnv() { return baseUrlEnv; }

    /** @return 模型环境变量名 */
    public String modelEnv() { return modelEnv; }
}
