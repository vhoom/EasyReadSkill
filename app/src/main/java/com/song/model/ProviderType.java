package com.song.model;

/**
 * 翻译服务类型。
 */
public enum ProviderType {
    BAIDU_FIELD("baidu_field", "百度领域翻译"),
    BAIDU_GENERAL("baidu_general", "百度通用翻译"),
    BAIDU_LLM("baidu_llm", "百度大模型翻译"),
    YOUDAO_TEXT("youdao_text", "有道文本翻译"),
    YOUDAO_LLM("youdao_llm", "有道大模型翻译"),
    DEEPSEEK_CHAT("deepseek_chat", "DeepSeek Chat"),
    ZHIPU_CHAT("zhipu_chat", "智谱 Chat"),
    GEMINI_CHAT("gemini_chat", "Gemini Chat"),
    KIMI_CHAT("kimi_chat", "Kimi Chat"),
    OPENAI_CHAT("openai_chat", "OpenAI Chat"),
    QIANWEN_CHAT("qianwen_chat", "通义千问 Chat");

    private final String id;
    private final String displayName;

    ProviderType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public ProviderVendor getVendor() {
        return switch (this) {
            case YOUDAO_TEXT, YOUDAO_LLM -> ProviderVendor.YOUDAO;
            case DEEPSEEK_CHAT -> ProviderVendor.DEEPSEEK;
            case ZHIPU_CHAT -> ProviderVendor.ZHIPU;
            case GEMINI_CHAT -> ProviderVendor.GEMINI;
            case KIMI_CHAT -> ProviderVendor.KIMI;
            case OPENAI_CHAT -> ProviderVendor.OPENAI;
            case QIANWEN_CHAT -> ProviderVendor.QIANWEN;
            default -> ProviderVendor.BAIDU;
        };
    }

    /** @return 该厂商下的默认服务类型 */
    public static ProviderType defaultFor(ProviderVendor vendor) {
        if (vendor == null) return getDefault();
        return switch (vendor) {
            case YOUDAO -> YOUDAO_TEXT;
            case DEEPSEEK -> DEEPSEEK_CHAT;
            case ZHIPU -> ZHIPU_CHAT;
            case GEMINI -> GEMINI_CHAT;
            case KIMI -> KIMI_CHAT;
            case OPENAI -> OPENAI_CHAT;
            case QIANWEN -> QIANWEN_CHAT;
            default -> BAIDU_FIELD;
        };
    }

    public static ProviderType getDefault() {
        return BAIDU_FIELD;
    }

    public static ProviderType fromId(String id) {
        if (id != null) {
            for (ProviderType type : values()) {
                if (type.id.equalsIgnoreCase(id)) return type;
            }
            if ("baidu".equalsIgnoreCase(id)) return BAIDU_GENERAL;
        }
        return getDefault();
    }

    @Override
    public String toString() { return displayName; }
}
