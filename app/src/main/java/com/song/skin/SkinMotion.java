package com.song.skin;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

/**
 * 换肤 100ms、对话框 200ms，只做透明度。按钮不缩放。
 */
public final class SkinMotion {

    private SkinMotion() {}

    /** 换肤后用 faster 淡入。颜色切换本身由样式表完成。 */
    public static void fadeSkinSwap(Parent root, Runnable apply) {
        if (apply != null) apply.run();
        if (root == null) return;
        root.setOpacity(0.92);
        FadeTransition in = new FadeTransition(Duration.millis(100), root);
        in.setFromValue(0.92);
        in.setToValue(1);
        in.play();
    }

    /** 对话框打开：normal 淡入。 */
    public static void fadeIn(Node node) {
        if (node == null) return;
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}
