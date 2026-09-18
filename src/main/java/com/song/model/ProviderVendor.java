package com.song.model;

/**
 * 翻译服务提供商。
 */
public enum ProviderVendor {
    BAIDU("baidu", "百度翻译"),
    YOUDAO("youdao", "有道智云");

    private final String id;
    private final String displayName;

    ProviderVendor(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

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