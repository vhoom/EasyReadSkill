package com.song.skin.animation;

import javafx.scene.control.Dialog;

/**
 * 弹窗打开动画安装器。
 */
public final class DialogAnimator {

    private DialogAnimator() {}

    public static void install(Dialog<?> dialog) {
        AnimationSupport.installDialog(dialog);
    }
}