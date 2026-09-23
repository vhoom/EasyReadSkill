package com.song.model;

public enum FilterType {
    ALL("全部"),
    UNTRANSLATED("未翻译"),
    TRANSLATED("已翻译"),
    FAILED("翻译失败");

    private final String label;

    FilterType(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}