package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.Button;

/**
 * 带轻量交互动画的按钮。
 */
public class AnimatedButton extends Button {

    public AnimatedButton() {
        super();
        AnimationSupport.installButton(this);
    }

    public AnimatedButton(String text) {
        super(text);
        AnimationSupport.installButton(this);
    }
}