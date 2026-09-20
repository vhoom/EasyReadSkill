package com.song.skin.animation;

/**
 * 动画强度。
 */
public enum AnimationLevel {
    OFF("关闭"),
    SUBTLE("轻微"),
    RICH("丰富");

    private final String label;

    AnimationLevel(String label) {
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