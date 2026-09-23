package com.llm.api;

/**
 * 单厂商连接参数。优先从 {@link LlmVendor} 对应环境变量读取。
 */
public final class LlmConfig {

    private final LlmVendor vendor;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    /**
     * @param vendor 厂商
     * @param baseUrl Base URL（自动去掉尾斜杠）
     * @param apiKey API Key
     * @param model 模型 id；空则用厂商默认
     */
    public LlmConfig(LlmVendor vendor, String baseUrl, String apiKey, String model) {
        if (vendor == null) {
            throw new IllegalArgumentException("vendor 不能为空");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey 不能为空");
        }
        this.vendor = vendor;
        this.baseUrl = trimSlash(baseUrl.trim());
        this.apiKey = apiKey.trim();
        this.model = (model == null || model.isBlank()) ? vendor.defaultModel() : model.trim();
    }

    /**
     * 从环境变量读取；缺 API Key 返回 null。
     *
     * @param vendor 厂商
     * @return 配置，或 null
     */
    public static LlmConfig fromEnv(LlmVendor vendor) {
        String key = firstEnv(vendor.apiKeyEnvs());
        if (key == null) {
            return null;
        }
        String base = env(vendor.baseUrlEnv(), vendor.defaultBaseUrl());
        String model = env(vendor.modelEnv(), vendor.defaultModel());
        return new LlmConfig(vendor, base, key, model);
    }

    /** @return 厂商 */
    public LlmVendor vendor() { return vendor; }

    /** @return Base URL */
    public String baseUrl() { return baseUrl; }

    /** @return API Key */
    public String apiKey() { return apiKey; }

    /** @return 模型 id */
    public String model() { return model; }

    /**
     * 复制并替换模型。
     *
     * @param model 新模型 id
     * @return 新配置
     */
    public LlmConfig withModel(String model) {
        return new LlmConfig(vendor, baseUrl, apiKey, model);
    }

    /**
     * 按序取第一个非空环境变量。
     *
     * @param names 变量名
     * @return 值或 null
     */
    public static String firstEnv(String... names) {
        for (String name : names) {
            String v = System.getenv(name);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /**
     * 读环境变量，缺省用默认值。
     *
     * @param name 变量名
     * @param def 默认值
     * @return 值
     */
    public static String env(String name, String def) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? def : v.trim();
    }

    /**
     * 去掉 URL 尾部斜杠。
     *
     * @param url 原始 URL
     * @return 规范化 URL
     */
    public static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
