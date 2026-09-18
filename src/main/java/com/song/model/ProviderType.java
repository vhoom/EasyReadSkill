package com.song.model;

/**
 * 翻译服务类型。
 */
public enum ProviderType {
    BAIDU_FIELD("baidu_field", "百度领域翻译"),
    BAIDU_GENERAL("baidu_general", "百度通用翻译"),
    BAIDU_LLM("baidu_llm", "百度大模型翻译"),
    YOUDAO_TEXT("youdao_text", "有道文本翻译"),
    YOUDAO_LLM("youdao_llm", "有道大模型翻译");

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
            default -> ProviderVendor.BAIDU;
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