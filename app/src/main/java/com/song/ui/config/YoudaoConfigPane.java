package com.song.ui.config;

import com.song.config.AppConfig;
import com.song.config.YoudaoConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.function.Consumer;

/**
 * 有道智云独立配置面板。
 */
public class YoudaoConfigPane extends GridPane {

    private final ComboBox<ProviderType> serviceBox = new ComboBox<>();
    private final TextField appIdField = new TextField();
    private final MaskedSecretField secretKeyField = new MaskedSecretField();
    private final ComboBox<String> domainBox = new ComboBox<>();
    private final ComboBox<String> handleOptionBox = new ComboBox<>();
    private final TextField promptField = new TextField();

    private final Label serviceLabel = new Label("服务类型：");
    private final Label appIdLabel = new Label("应用ID：");
    private final Label secretKeyLabel = new Label("应用密钥：");
    private final Label secretHintLabel = new Label("密钥失焦后写入配置并立即生效");
    private final Label domainLabel = new Label("文本翻译领域：");
    private final Label handleOptionLabel = new Label("大模型版本：");
    private final Label promptLabel = new Label("提示词：");
    private final Label promptHintLabel = new Label("系统默认");

    private Consumer<Void> onSecretCommitted;

    public YoudaoConfigPane() {
        setHgap(10);
        setVgap(12);
        setPadding(new Insets(4, 0, 4, 0));

        serviceBox.getItems().addAll(
                ProviderType.YOUDAO_TEXT,
                ProviderType.YOUDAO_LLM);
        serviceBox.setMaxWidth(Double.MAX_VALUE);
        appIdField.setPromptText("有道智云应用ID");
        secretKeyField.setPromptText("有道智云应用密钥");
        domainBox.getItems().addAll("general", "computers", "medicine", "finance", "game");
        domainBox.setMaxWidth(Double.MAX_VALUE);
        handleOptionBox.getItems().addAll(
                "deepseek-flash",
                "deepseek-v4.1-flash-expires-on-0910",
                "deepseek-v4-flash",
                "deepseek-v4-flash-vision-exp",
                "deepseek-v4-pro",
                "glm-5",
                "glm-5.1",
                "glm-5.2",
                "glm-5.3",
                "glm-5.3-flash",
                "kimi-k2.5",
                "kimi-k2.6",
                "kimi-k2.7-code",
                "kimi-k3",
                "mimo-v2.5",
                "mimo-v2.5-pro",
                "minimax-m2.5",
                "minimax-m2.7",
                "minimax-m3",
                "qwen3.5-plus",
                "qwen3.6-plus",
                "qwen3.7-flash",
                "qwen3.7-max",
                "qwen3.7-plus",
                "qwen3.8-27b",
                "qwen3.8-flash",
                "qwen3.8-max");
        handleOptionBox.setMaxWidth(Double.MAX_VALUE);
        promptField.setPromptText("可改写；不保存，下次启动恢复系统默认");
        secretHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        add(serviceLabel, 0, 0);
        add(serviceBox, 1, 0);
        add(appIdLabel, 0, 1);
        add(appIdField, 1, 1);
        add(secretKeyLabel, 0, 2);
        add(secretKeyField, 1, 2);
        add(secretHintLabel, 1, 3);
        add(domainLabel, 0, 4);
        add(domainBox, 1, 4);
        add(handleOptionLabel, 0, 5);
        add(handleOptionBox, 1, 5);
        add(promptLabel, 0, 6);
        javafx.scene.layout.HBox promptRow = new javafx.scene.layout.HBox(8, promptHintLabel, promptField);
        promptHintLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        promptHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        javafx.scene.layout.HBox.setHgrow(promptField, Priority.ALWAYS);
        promptField.setMaxWidth(Double.MAX_VALUE);
        add(promptRow, 1, 6);

        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        getColumnConstraints().addAll(left, right);

        serviceBox.valueProperty().addListener((obs, o, n) -> refreshFields());
        promptField.textProperty().addListener((obs, o, n) -> updatePromptHint());
        secretKeyField.setOnCommitted(ignored -> {
            if (onSecretCommitted != null) onSecretCommitted.accept(null);
        });
        refreshFields();
    }

    /**
     * 密钥失焦提交后回调。
     *
     * @param onSecretCommitted 回调
     */
    public void setOnSecretCommitted(Consumer<Void> onSecretCommitted) {
        this.onSecretCommitted = onSecretCommitted;
    }

    /**
     * 仅写入应用密钥并切换到有道厂商。
     *
     * @param config 应用配置
     */
    public void saveSecretsOnly(AppConfig config) {
        config.setVendor(ProviderVendor.YOUDAO);
        if (serviceBox.getValue() != null) {
            config.setProvider(serviceBox.getValue());
        }
        config.getYoudaoConfig().setSecretKey(secretKeyField.getPlain());
    }

    public void load(AppConfig config) {
        ProviderType provider = config.getProvider();
        if (provider == null || provider.getVendor() != ProviderVendor.YOUDAO) {
            provider = ProviderType.YOUDAO_TEXT;
        }
        serviceBox.setValue(provider);

        YoudaoConfig y = config.getYoudaoConfig();
        appIdField.setText(y.getAppId());
        secretKeyField.setPlain(y.getSecretKey());
        domainBox.setValue(y.getDomain());
        handleOptionBox.setValue(y.getHandleOption());
        promptField.setText(y.getPrompt());
        refreshFields();
    }

    public void save(AppConfig config) {
        config.setVendor(ProviderVendor.YOUDAO);
        config.setProvider(serviceBox.getValue());
        YoudaoConfig y = config.getYoudaoConfig();
        y.setAppId(text(appIdField));
        y.setSecretKey(secretKeyField.getPlain());
        y.setDomain(domainBox.getValue());
        y.setHandleOption(handleOptionBox.getValue());
        String promptText = text(promptField);
        y.setPrompt(promptText.isEmpty() ? YoudaoConfig.DEFAULT_LLM_PROMPT : promptText);
    }

    private void refreshFields() {
        ProviderType service = serviceBox.getValue();
        boolean isText = service == ProviderType.YOUDAO_TEXT;
        boolean isLlm = service == ProviderType.YOUDAO_LLM;
        show(appIdLabel, appIdField, true);
        show(secretKeyLabel, secretKeyField, true);
        secretHintLabel.setVisible(true);
        secretHintLabel.setManaged(true);
        show(domainLabel, domainBox, isText);
        show(handleOptionLabel, handleOptionBox, isLlm);
        show(promptLabel, promptField, isLlm);
        updatePromptHint();
    }

    private void updatePromptHint() {
        boolean isLlm = serviceBox.getValue() == ProviderType.YOUDAO_LLM;
        boolean isDefault = isDefaultPromptText(promptField.getText());
        boolean show = isLlm && isDefault;
        promptHintLabel.setVisible(show);
        promptHintLabel.setManaged(show);
    }

    private static boolean isDefaultPromptText(String text) {
        if (text == null || text.isBlank()) return true;
        return YoudaoConfig.DEFAULT_LLM_PROMPT.equals(text.trim());
    }

    private static void show(Node label, Node field, boolean visible) {
        label.setVisible(visible);
        label.setManaged(visible);
        field.setVisible(visible);
        field.setManaged(visible);
    }

    private static String text(javafx.scene.control.TextInputControl control) {
        return control.getText() == null ? "" : control.getText().trim();
    }
}
