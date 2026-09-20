package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.TextArea;

public class AnimatedTextArea extends TextArea {

    public AnimatedTextArea() {
        super();
        AnimationSupport.installTextInput(this);
    }
}