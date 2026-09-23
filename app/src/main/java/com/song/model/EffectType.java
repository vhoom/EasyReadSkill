package com.song.model;

public enum EffectType {
    OVERWRITE("替换原文"),
    KEEP_ENGLISH("翻译+原文");

    private final String label;

    EffectType(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}