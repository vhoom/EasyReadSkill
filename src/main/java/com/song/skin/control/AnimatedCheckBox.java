package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.CheckBox;

public class AnimatedCheckBox extends CheckBox {

    public AnimatedCheckBox() {
        super();
        AnimationSupport.installCheckBox(this);
    }

    public AnimatedCheckBox(String text) {
        super(text);
        AnimationSupport.installCheckBox(this);
    }
}