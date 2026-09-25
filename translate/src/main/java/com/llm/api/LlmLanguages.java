package com.llm.api;

import java.util.Locale;

/**
 * 语言代码 → 自然语言名，供大模型提示词使用。
 *
 * <p>大模型看不懂 {@code jp}/{@code kor}/{@code fra} 这类厂商码，
 * 提示词里必须写"日本語""한국어""法语"，否则会翻错语种或原样保留代码。</p>
 */
public final class LlmLanguages {

    /** 自动检测的展示名。 */
    public static final String AUTO = "自动检测";

    private LlmLanguages() {}

    /**
     * @param code 语言代码，可空
     * @return 自然语言名；未知代码原样返回
     */
    public static String displayName(String code) {
        if (code == null || code.isBlank()) {
            return AUTO;
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> AUTO;
            case "zh", "zh-chs", "chs" -> "简体中文";
            case "cht", "zh-cht", "zh-tw" -> "繁體中文";
            case "en" -> "English";
            case "jp", "ja" -> "日本語";
            case "kor", "ko" -> "한국어";
            case "fra", "fr" -> "法语";
            case "de" -> "德语";
            case "spa", "es" -> "西班牙语";
            case "ru" -> "俄语";
            case "pt" -> "葡萄牙语";
            case "it" -> "意大利语";
            case "ar" -> "阿拉伯语";
            case "hi" -> "印地语";
            case "th" -> "泰语";
            case "vi" -> "越南语";
            case "nl" -> "荷兰语";
            case "pl" -> "波兰语";
            case "tr" -> "土耳其语";
            case "uk" -> "乌克兰语";
            default -> code.trim();
        };
    }
}
