package com.song.skin.animation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * 全局动画配置。
 */
public final class AnimationSettings {

    private static final ObjectProperty<AnimationLevel> LEVEL =
            new SimpleObjectProperty<>(AnimationLevel.RICH);

    private AnimationSettings() {}

    public static ObjectProperty<AnimationLevel> levelProperty() {
        return LEVEL;
    }

    public static AnimationLevel getLevel() {
        return LEVEL.get();
    }

    public static void setLevel(AnimationLevel level) {
        LEVEL.set(level == null ? AnimationLevel.RICH : level);
    }
}