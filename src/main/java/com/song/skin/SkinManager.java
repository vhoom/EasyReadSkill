package com.song.skin;

import com.song.config.ConfigManager;
import com.song.skin.windows.WindowsTitleBar;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 皮肤管理器（Singleton + Observer + Facade + Memento）。
 * 负责创建、保存、应用皮肤，并通知监听者。
 */
public final class SkinManager {

    private static final Logger LOG = LoggerFactory.getLogger(SkinManager.class);
    private static final SkinManager INSTANCE = new SkinManager();

    private final SkinFactory factory = new DefaultSkinFactory();
    private final SkinCaretaker caretaker = new SkinCaretaker();
    private final ObjectProperty<AppSkin> skin = new SimpleObjectProperty<>();

    private Scene scene;
    private Stage stage;
    private Path cssFile;
    private String currentCssUrl;

    private SkinManager() {
        skin.set(factory.create(SkinType.LIGHT));
        skin.addListener((obs, oldSkin, newSkin) -> applyToScene(newSkin));
    }

    public static SkinManager getInstance() {
        return INSTANCE;
    }

    public ReadOnlyObjectProperty<AppSkin> skinProperty() {
        return skin;
    }

    public AppSkin getSkin() {
        return skin.get();
    }

    public SkinType getCurrentType() {
        AppSkin current = skin.get();
        return current != null ? current.getType() : SkinType.LIGHT;
    }

    public void setSkinType(SkinType type) {
        new SwitchSkinCommand(this, type).execute();
    }

    void switchTo(SkinType type) {
        caretaker.save(new SkinMemento(getCurrentType()));
        AppSkin next = factory.create(type);
        skin.set(next);
        LOG.info("切换皮肤: {}", next.getType().getLabel());
    }

    public void restorePreviousSkin() {
        SkinMemento memento = caretaker.restore();
        if (memento != null) {
            skin.set(factory.create(memento.type()));
        }
    }

    public void applyTo(Scene scene) {
        this.scene = scene;
        if (scene != null && scene.getWindow() instanceof Stage s) {
            this.stage = s;
        }
        applyToScene(skin.get());
    }

    /** 窗口显示后调用，用于应用 Windows 原生标题栏主题。 */
    public void attachStage(Stage stage) {
        this.stage = stage;
        applyWindowTitleBar();
    }

    private void applyToScene(AppSkin appSkin) {
        if (appSkin == null) return;

        if (scene == null) {
            LOG.warn("Scene 尚未绑定，暂只生成 CSS: {}", appSkin.getType().getLabel());
        } else {
            LOG.info("应用皮肤: {}", appSkin.getType().getLabel());
        }

        applyDirectStyles(appSkin.getTokens());
        applyWindowTitleBar();

        currentCssUrl = writeCssAndGetUrl(appSkin);
        if (scene != null && currentCssUrl != null) {
            scene.getStylesheets().setAll(currentCssUrl);
        }
    }

    private void applyWindowTitleBar() {
        if (stage == null || !stage.isShowing()) return;
        AppSkin current = skin.get();
        if (current == null) return;
        boolean dark = current.getType() != SkinType.LIGHT;
        WindowsTitleBar.apply(stage, current.getTokens(), dark);
    }

    private void applyDirectStyles(SkinTokens tokens) {
        if (scene == null) return;

        scene.setFill(Color.web(tokens.background()));

        Parent root = scene.getRoot();
        if (root == null) return;
        if (!root.getStyleClass().contains("root")) {
            root.getStyleClass().add("root");
        }

        root.setStyle("-fx-background-color: " + tokens.background() + ";");

        String topBarStyle = "-fx-background-color: " + tokens.surfaceAlt() + ";"
                + " -fx-border-color: " + tokens.border() + ";"
                + " -fx-border-width: 0 0 1 0;";
        root.lookupAll(".top-bar").forEach(node -> node.setStyle(topBarStyle));

        String leftPanelStyle = "-fx-background-color: " + tokens.surface() + ";"
                + " -fx-border-color: " + tokens.border() + ";"
                + " -fx-border-width: 0 1 0 0;";
        root.lookupAll(".left-panel").forEach(node -> node.setStyle(leftPanelStyle));

        String rightPanelStyle = "-fx-background-color: " + tokens.background() + ";";
        root.lookupAll(".right-panel").forEach(node -> node.setStyle(rightPanelStyle));

        String splitStyle = "-fx-background-color: " + tokens.background() + "; -fx-padding: 0;";
        root.lookupAll(".split-pane").forEach(node -> node.setStyle(splitStyle));

        String dividerStyle = "-fx-background-color: " + tokens.border() + "; -fx-padding: 0 1 0 1;";
        root.lookupAll(".split-pane-divider").forEach(node -> node.setStyle(dividerStyle));

        String listStyle = "-fx-background-color: " + tokens.surface() + ";"
                + " -fx-control-inner-background: " + tokens.surface() + ";"
                + " -fx-border-color: " + tokens.border() + ";";
        root.lookupAll(".list-view").forEach(node -> node.setStyle(listStyle));

        String inputStyle = "-fx-background-color: " + tokens.surface() + ";"
                + " -fx-control-inner-background: " + tokens.surface() + ";"
                + " -fx-text-fill: " + tokens.text() + ";"
                + " -fx-border-color: " + tokens.border() + ";";
        root.lookupAll(".text-area").forEach(node -> node.setStyle(inputStyle));
        root.lookupAll(".text-field").forEach(node -> node.setStyle(inputStyle));
        root.lookupAll(".combo-box").forEach(node -> node.setStyle(inputStyle));
    }

    public void applyTo(DialogPane dialogPane) {
        if (dialogPane == null) return;
        if (currentCssUrl == null) {
            currentCssUrl = writeCssAndGetUrl(skin.get());
        }
        if (currentCssUrl != null) {
            dialogPane.getStylesheets().setAll(currentCssUrl);
        }
        if (skin.get() != null) {
            dialogPane.setStyle("-fx-background-color: "
                    + skin.get().getTokens().surface() + ";");
        }
        bindDialogWindowTitleBar(dialogPane);
    }

    private void bindDialogWindowTitleBar(DialogPane dialogPane) {
        applyWindowTitleBar(dialogPane.getScene());
        dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            applyWindowTitleBar(newScene);
            newScene.windowProperty().addListener((obs2, oldWindow, newWindow) ->
                    applyWindowTitleBar(newWindow));
        });
    }

    private void applyWindowTitleBar(Scene dialogScene) {
        if (dialogScene != null) {
            applyWindowTitleBar(dialogScene.getWindow());
        }
    }

    private void applyWindowTitleBar(Window window) {
        if (!(window instanceof Stage dialogStage)) return;
        AppSkin current = skin.get();
        if (current == null) return;
        boolean dark = current.getType() != SkinType.LIGHT;
        Platform.runLater(() -> WindowsTitleBar.apply(
                dialogStage, current.getTokens(), dark));
    }

    private String writeCssAndGetUrl(AppSkin appSkin) {
        try {
            Path dir = ConfigManager.getDataDir().resolve("skin");
            Files.createDirectories(dir);

            // 使用唯一文件名，避免 JavaFX 按 URL 缓存旧样式表导致皮肤不刷新
            String fileName = "easyreadskill-" + appSkin.getType().name().toLowerCase()
                    + "-" + System.currentTimeMillis() + ".css";
            cssFile = dir.resolve(fileName);
            Files.writeString(cssFile, appSkin.getCss(), StandardCharsets.UTF_8);
            return cssFile.toUri().toString();
        } catch (IOException e) {
            LOG.error("写入皮肤 CSS 失败，回退到 data URI", e);
            String encoded = Base64.getEncoder()
                    .encodeToString(appSkin.getCss().getBytes(StandardCharsets.UTF_8));
            return "data:text/css;base64," + encoded;
        }
    }
}