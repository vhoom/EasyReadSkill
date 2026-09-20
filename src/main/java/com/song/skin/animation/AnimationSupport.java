package com.song.skin.animation;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * 组件动画工具。
 */
public final class AnimationSupport {

    private static final Duration FAST = Duration.millis(110);
    private static final Duration DIALOG = Duration.millis(180);

    private AnimationSupport() {}

    public static void installButton(Button button) {
        if (button == null) return;

        button.setScaleX(1);
        button.setScaleY(1);
        button.setTranslateY(0);

        boolean[] hovered = {false};

        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (button.isDisabled()) return;
            hovered[0] = true;
            scale(button, AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.05 : 1.03);
        });

        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hovered[0] = false;
            scale(button, 1.0);
            translateY(button, 0);
        });

        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (button.isDisabled()) return;
            scale(button, 0.97);
            translateY(button, 1);
        });

        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (button.isDisabled()) return;
            scale(button, hovered[0]
                    ? (AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.05 : 1.03)
                    : 1.0);
            translateY(button, 0);
        });
    }

    public static void installListCell(ListCell<?> cell) {
        if (cell == null) return;

        cell.setTranslateX(0);
        boolean[] hovered = {false};

        cell.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            hovered[0] = true;
            translateX(cell, AnimationSettings.getLevel() == AnimationLevel.RICH ? 5 : 3);
        });

        cell.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hovered[0] = false;
            translateX(cell, 0);
        });
    }

    public static void installCheckBox(CheckBox checkBox) {
        if (checkBox == null) return;
        checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> pulse(checkBox));
    }

    public static void installComboBox(ComboBox<?> comboBox) {
        if (comboBox == null) return;

        comboBox.setScaleX(1);
        comboBox.setScaleY(1);
        boolean[] hovered = {false};

        comboBox.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (comboBox.isDisabled()) return;
            hovered[0] = true;
            scale(comboBox, AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.03 : 1.015);
        });

        comboBox.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hovered[0] = false;
            scale(comboBox, 1.0);
        });

        comboBox.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (comboBox.isDisabled()) return;
            scale(comboBox, 0.985);
        });

        comboBox.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (comboBox.isDisabled()) return;
            scale(comboBox, hovered[0]
                    ? (AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.03 : 1.015)
                    : 1.0);
        });
    }

    public static void installTextInput(TextInputControl control) {
        if (control == null) return;
        control.focusedProperty().addListener((obs, oldValue, focused) ->
                scale(control, focused ? 1.01 : 1.0));
    }

    private static void pulse(Node node) {
        if (AnimationSettings.getLevel() == AnimationLevel.OFF) return;

        ScaleTransition down = new ScaleTransition(Duration.millis(70), node);
        down.setToX(0.90);
        down.setToY(0.90);
        down.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition up = new ScaleTransition(Duration.millis(120), node);
        up.setToX(AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.15 : 1.08);
        up.setToY(AnimationSettings.getLevel() == AnimationLevel.RICH ? 1.15 : 1.08);
        up.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition back = new ScaleTransition(Duration.millis(120), node);
        back.setToX(1);
        back.setToY(1);
        back.setInterpolator(Interpolator.EASE_BOTH);

        new SequentialTransition(down, up, back).play();
    }

    public static void installDialog(Dialog<?> dialog) {
        if (dialog == null) return;

        DialogPane pane = dialog.getDialogPane();

        dialog.setOnShowing(e -> {
            if (AnimationSettings.getLevel() == AnimationLevel.OFF) {
                resetDialog(pane);
                return;
            }
            pane.setOpacity(0);
            pane.setScaleX(0.96);
            pane.setScaleY(0.96);
        });

        dialog.setOnShown(e -> {
            if (AnimationSettings.getLevel() == AnimationLevel.OFF) {
                resetDialog(pane);
                return;
            }

            FadeTransition fade = new FadeTransition(DIALOG, pane);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            ScaleTransition scale = new ScaleTransition(DIALOG, pane);
            scale.setFromX(0.96);
            scale.setFromY(0.96);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_BOTH);

            new ParallelTransition(fade, scale).play();
        });
    }

    private static void resetDialog(DialogPane pane) {
        pane.setOpacity(1);
        pane.setScaleX(1);
        pane.setScaleY(1);
    }

    private static void scale(Node node, double to) {
        if (AnimationSettings.getLevel() == AnimationLevel.OFF) return;
        ScaleTransition transition = new ScaleTransition(FAST, node);
        transition.setToX(to);
        transition.setToY(to);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.playFromStart();
    }

    private static void translateY(Node node, double to) {
        if (AnimationSettings.getLevel() == AnimationLevel.OFF) return;
        TranslateTransition transition = new TranslateTransition(FAST, node);
        transition.setToY(to);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.playFromStart();
    }

    private static void translateX(Node node, double to) {
        if (AnimationSettings.getLevel() == AnimationLevel.OFF) return;
        TranslateTransition transition = new TranslateTransition(FAST, node);
        transition.setToX(to);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.playFromStart();
    }
}