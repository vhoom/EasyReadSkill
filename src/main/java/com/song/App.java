package com.song;

import com.song.skin.SkinManager;
import com.song.ui.AppState;
import com.song.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        AppState state = new AppState();
        MainView root = new MainView(state);
        Scene scene = new Scene(root, 1200, 780);
        SkinManager.getInstance().applyTo(scene);
        stage.setTitle("EasyReadSkill");
        stage.setScene(scene);
        stage.show();
        SkinManager.getInstance().attachStage(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}