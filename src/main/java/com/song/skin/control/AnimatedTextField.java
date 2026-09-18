package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.TextField;

public class AnimatedTextField extends TextField {

    public AnimatedTextField() {
        super();
        AnimationSupport.installTextInput(this);
    }

    public AnimatedTextField(String text) {
        super(text);
        AnimationSupport.installTextInput(this);
    }
}