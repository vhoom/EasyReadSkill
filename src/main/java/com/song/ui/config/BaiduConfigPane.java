package com.song.ui.config;

import com.song.config.AppConfig;
import com.song.config.BaiduConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.skin.control.AnimatedComboBox;
import com.song.skin.control.AnimatedPasswordField;
import com.song.skin.control.AnimatedTextField;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * 百度翻译独立配置面板。
 */
public class BaiduConfigPane extends GridPane {

    private final AnimatedComboBox<ProviderType> serviceBox = new AnimatedComboBox<>();
    private final AnimatedTextField appIdField = new AnimatedTextField();
    private final AnimatedPasswordField apiKeyField = new AnimatedPasswordField();
    private final AnimatedPasswordField secretKeyField = new AnimatedPasswordField();
    private final AnimatedComboBox<String> domainBox = new AnimatedComboBox<>();

    private final Label serviceLabel = new Label("服务类型：");
    private final Label appIdLabel = new Label("APPID：");
    private final Label apiKeyLabel = new Label("API Key：");
    private final Label secretKeyLabel = new Label("平台密钥：");
    private final Label domainLabel = new Label("翻译领域：");

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

        add(serviceLabel, 0, 0);
        add(serviceBox, 1, 0);
        add(appIdLabel, 0, 1);
        add(appIdField, 1, 1);
        add(apiKeyLabel, 0, 2);
        add(apiKeyField, 1, 2);
        add(secretKeyLabel, 0, 3);
        add(secretKeyField, 1, 3);
        add(domainLabel, 0, 4);
        add(domainBox, 1, 4);

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
        if (provider == null || provider.getVendor() != ProviderVendor.BAIDU) {
            provider = ProviderType.BAIDU_FIELD;
        }
        serviceBox.setValue(provider);

        BaiduConfig b = config.getBaiduConfig();
        appIdField.setText(b.getAppId());
        apiKeyField.setText(b.getApiKey());
        secretKeyField.setText(b.getSecretKey());
        domainBox.setValue(b.getDomain());
        refreshFields();
    }

    public void save(AppConfig config) {
        config.setVendor(ProviderVendor.BAIDU);
        config.setProvider(serviceBox.getValue());
        BaiduConfig b = config.getBaiduConfig();
        b.setAppId(text(appIdField));
        b.setApiKey(text(apiKeyField));
        b.setSecretKey(text(secretKeyField));
        b.setDomain(domainBox.getValue());
    }

    private void refreshFields() {
        ProviderType service = serviceBox.getValue();
        boolean isField = service == ProviderType.BAIDU_FIELD;
        boolean isLlm = service == ProviderType.BAIDU_LLM;
        show(appIdLabel, appIdField, true);
        show(apiKeyLabel, apiKeyField, isLlm);
        show(secretKeyLabel, secretKeyField, true);
        show(domainLabel, domainBox, isField);
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