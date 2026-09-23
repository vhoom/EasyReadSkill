package com.song.util;

/**
 * 语言代码与本地语言显示名称。
 */
public final class LanguageUtils {

    private LanguageUtils() {}

    public static String displayName(String code) {
        if (code == null) return "";
        return switch (code) {
            case "auto" -> "Auto";
            case "zh", "zh-CHS" -> "简体中文";
            case "cht", "zh-CHT" -> "繁體中文";
            case "en" -> "English";
            case "jp", "ja" -> "日本語";
            case "kor", "ko" -> "한국어";
            case "fra", "fr" -> "Français";
            case "de" -> "Deutsch";
            case "spa", "es" -> "Español";
            case "ru" -> "Русский";
            case "pt" -> "Português";
            case "it" -> "Italiano";
            case "ar" -> "العربية";
            case "hi" -> "हनद";
            case "th" -> "ไทย";
            case "vi" -> "Tiếng Việt";
            case "nl" -> "Nederlands";
            case "pl" -> "Polski";
            case "tr" -> "Türkçe";
            case "uk" -> "Українська";
            default -> code;
        };
    }
}