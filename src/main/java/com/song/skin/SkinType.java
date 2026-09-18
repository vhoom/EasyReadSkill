package com.song.skin;

/**
 * 皮肤类型。
 */
public enum SkinType {
    LIGHT("浅色"),
    DARK("深色"),
    HIGH_CONTRAST("高对比度");

    private final String label;

    SkinType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}