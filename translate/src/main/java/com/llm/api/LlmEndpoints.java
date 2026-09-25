package com.llm.api;

/**
 * OpenAI 兼容接口地址。
 *
 * <p>把用户在界面上填的 Base URL 规范成可直接拼路径的地址：</p>
 * <ul>
 *   <li>去掉尾部斜杠</li>
 *   <li>剥掉误填的 {@code /chat/completions}、{@code /completions}、{@code /models} 尾巴</li>
 *   <li>路径含 {@code /v1/} 而 Base 不含时补上 {@code /v1}</li>
 * </ul>
 *
 * <p>纯函数，无状态，线程安全。</p>
 */
public final class LlmEndpoints {

    private static final String[] REDUNDANT_SUFFIXES = {
            "/chat/completions", "/completions", "/models"
    };

    private LlmEndpoints() {}

    /**
     * 规范化 Base URL（不补 /v1）。
     *
     * @param baseUrl 用户输入
     * @return 规范化后的 Base；输入为空返回空串
     */
    public static String normalizeBase(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String base = baseUrl.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            base = trimTrailingSlashes(base);
            for (String suffix : REDUNDANT_SUFFIXES) {
                if (base.endsWith(suffix)) {
                    base = base.substring(0, base.length() - suffix.length());
                    changed = true;
                }
            }
        }
        return trimTrailingSlashes(base);
    }

    /**
     * 可直接拼相对路径的 Base（按需补 {@code /v1}）。
     *
     * @param vendor  厂商；为空则不补 {@code /v1}
     * @param baseUrl 用户输入；为空则用厂商默认
     * @return Base URL
     */
    public static String clientBase(LlmVendor vendor, String baseUrl) {
        String base = normalizeBase(baseUrl);
        if (base.isEmpty()) {
            if (vendor == null) {
                return "";
            }
            base = vendor.defaultBaseUrl();
        }
        if (vendor != null
                && vendor.completionsPath().startsWith("/v1/")
                && !base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base;
    }

    /**
     * {@code /models} 完整地址。
     *
     * @param vendor  厂商
     * @param baseUrl 用户输入
     * @return 完整地址
     */
    public static String modelsUrl(LlmVendor vendor, String baseUrl) {
        String base = clientBase(vendor, baseUrl);
        String path = vendor == null ? "/models" : vendor.modelsPath();
        return base + relativePath(path, base);
    }

    /**
     * {@code /chat/completions} 完整地址。
     *
     * @param vendor  厂商
     * @param baseUrl 用户输入
     * @return 完整地址
     */
    public static String chatUrl(LlmVendor vendor, String baseUrl) {
        String base = clientBase(vendor, baseUrl);
        String path = vendor == null ? "/v1/chat/completions" : vendor.completionsPath();
        return base + relativePath(path, base);
    }

    private static String relativePath(String path, String clientBase) {
        if (clientBase.endsWith("/v1") && path.startsWith("/v1/")) {
            return path.substring(3);
        }
        return path;
    }

    private static String trimTrailingSlashes(String value) {
        String out = value;
        while (out.endsWith("/") && out.length() > 1) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
