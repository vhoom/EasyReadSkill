package com.song.ui.config;

import com.song.config.AppConfig;
import com.song.config.YoudaoConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * 有道智云独立配置面板。
 */
public class YoudaoConfigPane extends GridPane {

    private final ComboBox<ProviderType> serviceBox = new ComboBox<>();
    private final TextField appIdField = new TextField();
    private final PasswordField secretKeyField = new PasswordField();
    private final ComboBox<String> domainBox = new ComboBox<>();
    private final ComboBox<String> handleOptionBox = new ComboBox<>();
    private final TextField promptField = new TextField();

    private final Label serviceLabel = new Label("服务类型：");
    private final Label appIdLabel = new Label("应用ID：");
    private final Label secretKeyLabel = new Label("应用密钥：");
    private final Label domainLabel = new Label("文本翻译领域：");
    private final Label handleOptionLabel = new Label("大模型版本：");
    private final Label promptLabel = new Label("提示词：");

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
        promptField.setPromptText("可选提示词，最多 1200 字符 / 400 单词");

        add(serviceLabel, 0, 0);
        add(serviceBox, 1, 0);
        add(appIdLabel, 0, 1);
        add(appIdField, 1, 1);
        add(secretKeyLabel, 0, 2);
        add(secretKeyField, 1, 2);
        add(domainLabel, 0, 3);
        add(domainBox, 1, 3);
        add(handleOptionLabel, 0, 4);
        add(handleOptionBox, 1, 4);
        add(promptLabel, 0, 5);
        add(promptField, 1, 5);

        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        getColumnConstraints().addAll(left, right);

        serviceBox.valueProperty().addListener((obs, o, n) -> refreshFields());
        refreshFields();
    }

    public void load(AppConfig config) {
        ProviderType provider = config.getProvider();
        if (provider == null || provider.getVendor() != ProviderVendor.YOUDAO) {
            provider = ProviderType.YOUDAO_TEXT;
        }
        serviceBox.setValue(provider);

        YoudaoConfig y = config.getYoudaoConfig();
        appIdField.setText(y.getAppId());
        secretKeyField.setText(y.getSecretKey());
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
        y.setSecretKey(text(secretKeyField));
        y.setDomain(domainBox.getValue());
        y.setHandleOption(handleOptionBox.getValue());
        y.setPrompt(text(promptField));
    }

    private void refreshFields() {
        ProviderType service = serviceBox.getValue();
        boolean isText = service == ProviderType.YOUDAO_TEXT;
        boolean isLlm = service == ProviderType.YOUDAO_LLM;
        show(appIdLabel, appIdField, true);
        show(secretKeyLabel, secretKeyField, true);
        show(domainLabel, domainBox, isText);
        show(handleOptionLabel, handleOptionBox, isLlm);
        show(promptLabel, promptField, isLlm);
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