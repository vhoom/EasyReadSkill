package com.song.skin.control;

import com.song.skin.animation.AnimationSupport;

import javafx.scene.control.ListCell;

/**
 * 带悬停位移动画的 ListCell 基类。
 */
public abstract class AnimatedListCell<T> extends ListCell<T> {

    protected AnimatedListCell() {
        AnimationSupport.installListCell(this);
    }
}