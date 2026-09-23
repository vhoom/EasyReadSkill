package com.song;

import com.song.config.AppConfig;
import com.song.skin.SkinManager;
import com.song.ui.AppState;
import com.song.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private AppState state;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.state = new AppState();
        AppConfig config = state.getConfig();

        MainView root = new MainView(state);

        double width = config.getWindowWidth() > 300 ? config.getWindowWidth() : 1200;
        double height = config.getWindowHeight() > 200 ? config.getWindowHeight() : 780;
        Scene scene = new Scene(root, width, height);

        if (config.getWindowX() >= 0 && config.getWindowY() >= 0) {
            stage.setX(config.getWindowX());
            stage.setY(config.getWindowY());
        }
        stage.setMaximized(config.isWindowMaximized());

        SkinManager.getInstance().applyTo(scene);
        stage.setTitle("EasyReadSkill");
        stage.setScene(scene);
        stage.show();
        SkinManager.getInstance().attachStage(stage);
    }

    @Override
    public void stop() {
        if (state != null) {
            state.saveUiState(stage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}