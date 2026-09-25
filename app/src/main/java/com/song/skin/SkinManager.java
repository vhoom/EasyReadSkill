package com.song.skin;

import com.song.config.ConfigManager;
import com.song.skin.windows.WindowsTitleBar;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 皮肤管理：切换主题、写 CSS、应用到 Scene / Dialog。
 */
public final class SkinManager {

    private static final Logger LOG = LoggerFactory.getLogger(SkinManager.class);
    private static final SkinManager INSTANCE = new SkinManager();

    private final ObjectProperty<AppSkin> skin = new SimpleObjectProperty<>();

    private Scene scene;
    private Stage stage;
    private String currentCssUrl;

    private SkinManager() {
        skin.set(AppSkin.of(SkinType.LIGHT));
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
        SkinType next = type != null ? type : SkinType.LIGHT;
        if (next == getCurrentType() && skin.get() != null) {
            applyToScene(skin.get());
            return;
        }
        LOG.info("切换皮肤: {}", next.getLabel());
        Parent root = scene != null ? scene.getRoot() : null;
        if (root != null) {
            SkinMotion.fadeSkinSwap(root, () -> skin.set(AppSkin.of(next)));
        } else {
            skin.set(AppSkin.of(next));
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
        WindowsTitleBar.apply(stage, current.getTokens(), current.getType().isDarkChrome());
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

        String topBarStyle = "-fx-background-color: " + tokens.background() + ";"
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

        String dividerStyle = "-fx-background-color: " + tokens.border() + "; -fx-padding: 0 0.5 0 0.5;";
        root.lookupAll(".split-pane-divider").forEach(node -> node.setStyle(dividerStyle));

        String listStyle = "-fx-background-color: " + tokens.surface() + ";"
                + " -fx-border-color: transparent; -fx-padding: 4 0 8 0;";
        root.lookupAll(".list-view").forEach(node -> node.setStyle(listStyle));

        paintLabeled(root, tokens);
        paintScrollBars(root, tokens);
    }

    /** 强制标签/单选/复选字色，避免 Modena 深色下仍用近黑字。列表单元格由 LeftPanel 自己上色。 */
    private static void paintLabeled(Parent root, SkinTokens tokens) {
        Color text = Color.web(tokens.text());
        Color secondary = Color.web(tokens.textSecondary());
        for (Node node : root.lookupAll(".label, .radio-button, .check-box")) {
            if (!(node instanceof Labeled labeled)) continue;
            if (underStyleClass(node, "list-cell")) continue;
            if (labeled.getStyleClass().contains("filter-tab") && labeled instanceof RadioButton tab) {
                paintFilterTab(tab, text, secondary);
            } else if (labeled.getStyleClass().contains("tag-warn")) {
                // 非外部 skill 标记：保留警示色，不被通用文字色覆盖
                labeled.setTextFill(Color.web(tokens.warning()));
            } else if (labeled.getStyleClass().contains("secondary")
                    || labeled.getStyleClass().contains("eyebrow")) {
                labeled.setTextFill(secondary);
            } else {
                labeled.setTextFill(text);
            }
        }
    }

    private static void paintFilterTab(RadioButton tab, Color text, Color secondary) {
        tab.setTextFill(tab.isSelected() ? text : secondary);
        if (tab.getProperties().putIfAbsent("filter-paint", Boolean.TRUE) != null) return;
        tab.selectedProperty().addListener((obs, old, selected) -> {
            AppSkin current = getInstance().getSkin();
            if (current == null) return;
            SkinTokens tokens = current.getTokens();
            tab.setTextFill(Color.web(selected ? tokens.text() : tokens.textSecondary()));
        });
    }

    private static boolean underStyleClass(Node node, String styleClass) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n.getStyleClass().contains(styleClass)) return true;
        }
        return false;
    }

    private static void paintScrollBars(Parent root, SkinTokens tokens) {
        String clear = "-fx-background-color: transparent; -fx-padding: 0;";
        String thumbStyle = "-fx-background-color: " + tokens.scrollThumb() + ";"
                + " -fx-background-insets: 1; -fx-background-radius: 999;";
        for (Node node : root.lookupAll(".scroll-bar")) {
            node.setStyle(clear);
            if (node instanceof ScrollBar) {
                for (Node child : ((Parent) node).lookupAll(".thumb")) {
                    child.setStyle(thumbStyle);
                }
                for (Node child : ((Parent) node).lookupAll(".track, .increment-button, .decrement-button")) {
                    child.setStyle(clear);
                }
            }
        }
        for (Node node : root.lookupAll(".scroll-pane > .corner")) {
            node.setStyle(clear);
        }
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
            SkinTokens tokens = skin.get().getTokens();
            dialogPane.setStyle("-fx-background-color: " + tokens.surface() + ";");
            paintLabeled(dialogPane, tokens);
            paintScrollBars(dialogPane, tokens);
        }
        SkinMotion.fadeIn(dialogPane);
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
        Platform.runLater(() -> WindowsTitleBar.apply(
                dialogStage, current.getTokens(), current.getType().isDarkChrome()));
    }

    private String writeCssAndGetUrl(AppSkin appSkin) {
        try {
            Path dir = ConfigManager.getDataDir().resolve("skin");
            Files.createDirectories(dir);

            Path cssFile = dir.resolve("skin-" + System.nanoTime() + ".css");
            Files.writeString(cssFile, appSkin.getCss(), StandardCharsets.UTF_8);
            deleteStaleCss(dir, cssFile);
            return cssFile.toUri().toString();
        } catch (IOException e) {
            LOG.error("写入皮肤 CSS 失败，回退到 data URI", e);
            String encoded = Base64.getEncoder()
                    .encodeToString(appSkin.getCss().getBytes(StandardCharsets.UTF_8));
            return "data:text/css;base64," + encoded;
        }
    }

    private static void deleteStaleCss(Path dir, Path keep) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.css")) {
            for (Path p : stream) {
                if (!p.equals(keep)) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best-effort cleanup
                    }
                }
            }
        } catch (IOException e) {
            LOG.debug("清理旧皮肤 CSS 失败: {}", e.toString());
        }
    }
}
