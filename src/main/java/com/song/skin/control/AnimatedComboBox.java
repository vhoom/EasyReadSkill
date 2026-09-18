package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.ComboBox;

public class AnimatedComboBox<T> extends ComboBox<T> {

    public AnimatedComboBox() {
        super();
        AnimationSupport.installComboBox(this);
    }
}