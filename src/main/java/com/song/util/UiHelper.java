package com.song.util;

import com.song.skin.SkinManager;
import com.song.skin.animation.DialogAnimator;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class UiHelper {
    public static void info(String title, String msg) {
        show(Alert.AlertType.INFORMATION, title, msg);
    }

    public static void warn(String title, String msg) {
        show(Alert.AlertType.WARNING, title, msg);
    }

    public static void error(String title, String msg) {
        show(Alert.AlertType.ERROR, title, msg);
    }

    public static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        prepare(a, title, msg);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        prepare(a, title, msg);
        a.showAndWait();
    }

    private static void prepare(Alert a, String title, String msg) {
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        SkinManager.getInstance().applyTo(a.getDialogPane());
        DialogAnimator.install(a);
    }
}