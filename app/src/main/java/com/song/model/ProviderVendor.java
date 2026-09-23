package com.song.model;

/**
 * 翻译服务提供商（百度 / 有道 / OpenAI 兼容大模型）。
 */
public enum ProviderVendor {
    BAIDU("baidu", "百度翻译", false),
    YOUDAO("youdao", "有道智云", false),
    DEEPSEEK("deepseek", "DeepSeek", true),
    ZHIPU("zhipu", "智谱 GLM", true),
    GEMINI("gemini", "Gemini", true),
    KIMI("kimi", "Kimi", true),
    OPENAI("openai", "OpenAI", true),
    QIANWEN("qianwen", "通义千问", true);

    private final String id;
    private final String displayName;
    private final boolean llm;

    ProviderVendor(String id, String displayName, boolean llm) {
        this.id = id;
        this.displayName = displayName;
        this.llm = llm;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    /** @return 是否为 OpenAI 兼容大模型厂商 */
    public boolean isLlm() { return llm; }

    public static ProviderVendor fromId(String id) {
        if (id != null) {
            for (ProviderVendor vendor : values()) {
                if (vendor.id.equalsIgnoreCase(id)) return vendor;
            }
        }
        return BAIDU;
    }

    @Override
    public String toString() { return displayName; }
}
