package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.PasswordField;

public class AnimatedPasswordField extends PasswordField {

    public AnimatedPasswordField() {
        super();
        AnimationSupport.installTextInput(this);
    }
}