package com.song.ui.config;

import com.song.config.AppConfig;
import com.song.config.BaiduConfig;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * 百度翻译独立配置面板。
 */
public class BaiduConfigPane extends GridPane {

    private final ComboBox<ProviderType> serviceBox = new ComboBox<>();
    private final TextField appIdField = new TextField();
    private final MaskedSecretField apiKeyField = new MaskedSecretField();
    private final MaskedSecretField secretKeyField = new MaskedSecretField();
    private final ComboBox<String> domainBox = new ComboBox<>();
    private final TextField promptField = new TextField();

    private final Label serviceLabel = new Label("服务类型：");
    private final Label appIdLabel = new Label("APPID：");
    private final Label apiKeyLabel = new Label("API Key：");
    private final Label secretKeyLabel = new Label("平台密钥：");
    private final Label domainLabel = new Label("翻译领域：");
    private final Label promptLabel = new Label("提示词：");
    private final Label promptHintLabel = new Label("系统默认");
    private final Label secretHintLabel = new Label("密钥失焦后写入配置并立即生效");

    private Consumer<Void> onSecretCommitted;

    public BaiduConfigPane() {
        setHgap(10);
        setVgap(12);
        setPadding(new Insets(4, 0, 4, 0));

        serviceBox.getItems().addAll(
                ProviderType.BAIDU_FIELD,
                ProviderType.BAIDU_GENERAL,
                ProviderType.BAIDU_LLM);
        serviceBox.setMaxWidth(Double.MAX_VALUE);
        appIdField.setPromptText("百度翻译平台 APPID");
        apiKeyField.setPromptText("百度大模型 API Key");
        secretKeyField.setPromptText("百度翻译平台密钥");
        domainBox.getItems().addAll(
                "it", "finance", "machinery", "senimed", "novel",
                "academic", "aerospace", "wiki", "news", "law", "contract");
        domainBox.setMaxWidth(Double.MAX_VALUE);
        promptField.setPromptText("可改写；不保存，下次启动恢复系统默认");
        secretHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        add(serviceLabel, 0, 0);
        add(serviceBox, 1, 0);
        add(appIdLabel, 0, 1);
        add(appIdField, 1, 1);
        add(apiKeyLabel, 0, 2);
        add(apiKeyField, 1, 2);
        add(secretKeyLabel, 0, 3);
        add(secretKeyField, 1, 3);
        add(secretHintLabel, 1, 4);
        add(domainLabel, 0, 5);
        add(domainBox, 1, 5);
        add(promptLabel, 0, 6);
        HBox promptRow = new HBox(8, promptHintLabel, promptField);
        promptHintLabel.setMinWidth(Region.USE_PREF_SIZE);
        promptHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        HBox.setHgrow(promptField, Priority.ALWAYS);
        promptField.setMaxWidth(Double.MAX_VALUE);
        add(promptRow, 1, 6);

        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        getColumnConstraints().addAll(left, right);

        serviceBox.valueProperty().addListener((obs, o, n) -> refreshFields());
        promptField.textProperty().addListener((obs, o, n) -> updatePromptHint());
        Consumer<String> commit = ignored -> {
            if (onSecretCommitted != null) onSecretCommitted.accept(null);
        };
        apiKeyField.setOnCommitted(commit);
        secretKeyField.setOnCommitted(commit);
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
     * 仅写入密钥相关字段并切换到百度厂商。
     *
     * @param config 应用配置
     */
    public void saveSecretsOnly(AppConfig config) {
        config.setVendor(ProviderVendor.BAIDU);
        if (serviceBox.getValue() != null) {
            config.setProvider(serviceBox.getValue());
        }
        BaiduConfig b = config.getBaiduConfig();
        b.setApiKey(apiKeyField.getPlain());
        b.setSecretKey(secretKeyField.getPlain());
    }

    public void load(AppConfig config) {
        ProviderType provider = config.getProvider();
        if (provider == null || provider.getVendor() != ProviderVendor.BAIDU) {
            provider = ProviderType.BAIDU_FIELD;
        }
        serviceBox.setValue(provider);

        BaiduConfig b = config.getBaiduConfig();
        appIdField.setText(b.getAppId());
        apiKeyField.setPlain(b.getApiKey());
        secretKeyField.setPlain(b.getSecretKey());
        domainBox.setValue(b.getDomain());
        promptField.setText(b.getPrompt());
        refreshFields();
    }

    public void save(AppConfig config) {
        config.setVendor(ProviderVendor.BAIDU);
        config.setProvider(serviceBox.getValue());
        BaiduConfig b = config.getBaiduConfig();
        b.setAppId(text(appIdField));
        b.setApiKey(apiKeyField.getPlain());
        b.setSecretKey(secretKeyField.getPlain());
        b.setDomain(domainBox.getValue());
        String promptText = text(promptField);
        b.setPrompt(promptText.isEmpty() ? YoudaoConfig.DEFAULT_LLM_PROMPT : promptText);
    }

    private void refreshFields() {
        ProviderType service = serviceBox.getValue();
        boolean isField = service == ProviderType.BAIDU_FIELD;
        boolean isLlm = service == ProviderType.BAIDU_LLM;
        show(appIdLabel, appIdField, true);
        show(apiKeyLabel, apiKeyField, isLlm);
        show(secretKeyLabel, secretKeyField, true);
        secretHintLabel.setVisible(true);
        secretHintLabel.setManaged(true);
        show(domainLabel, domainBox, isField);
        show(promptLabel, promptField, isLlm);
        updatePromptHint();
    }

    private void updatePromptHint() {
        boolean isLlm = serviceBox.getValue() == ProviderType.BAIDU_LLM;
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
