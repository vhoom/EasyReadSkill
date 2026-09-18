package com.song.service.factory;

/**
 * 项目内语言代码 -> 有道语言代码。
 */
final class YoudaoLanguageMapper {

    private YoudaoLanguageMapper() {}

    static String map(String lang) {
        if (lang == null) return "auto";
        return switch (lang) {
            case "zh" -> "zh-CHS";
            case "cht", "zh-CHT" -> "zh-CHT";
            case "jp" -> "ja";
            case "kor" -> "ko";
            case "fra" -> "fr";
            case "spa" -> "es";
            case "de" -> "de";
            case "ru" -> "ru";
            case "pt" -> "pt";
            case "it" -> "it";
            default -> lang;
        };
    }
}