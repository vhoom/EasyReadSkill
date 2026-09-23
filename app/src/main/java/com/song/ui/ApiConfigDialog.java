package com.song.ui;

import com.song.config.AppConfig;
import com.song.model.ProviderVendor;
import com.song.skin.SkinManager;
import com.song.ui.config.BaiduConfigPane;
import com.song.ui.config.LlmConfigPane;
import com.song.ui.config.YoudaoConfigPane;
import com.song.util.LanguageUtils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.Map;

/**
 * 顶部 API 配置入口。
 * 不同提供商使用独立配置面板。
 */
public class ApiConfigDialog extends Dialog<Void> {

    public ApiConfigDialog(AppState state) {
        AppConfig config = state.getConfig();

        setTitle("API 配置");
        setResizable(true);

        ComboBox<ProviderVendor> vendorBox = new ComboBox<>();
        vendorBox.getItems().setAll(ProviderVendor.values());
        vendorBox.setValue(config.getVendor());
        vendorBox.setMaxWidth(Double.MAX_VALUE);

        BaiduConfigPane baiduPane = new BaiduConfigPane();
        YoudaoConfigPane youdaoPane = new YoudaoConfigPane();
        baiduPane.load(config);
        youdaoPane.load(config);
        Runnable persistSecrets = () -> {
            state.saveConfig();
            state.getTranslationService().refreshProvider();
        };
        baiduPane.setOnSecretCommitted(ignored -> {
            baiduPane.saveSecretsOnly(config);
            persistSecrets.run();
        });
        youdaoPane.setOnSecretCommitted(ignored -> {
            youdaoPane.saveSecretsOnly(config);
            persistSecrets.run();
        });

        Map<ProviderVendor, LlmConfigPane> llmPanes = new EnumMap<>(ProviderVendor.class);
        for (ProviderVendor v : ProviderVendor.values()) {
            if (!v.isLlm()) continue;
            LlmConfigPane pane = new LlmConfigPane(v);
            pane.load(config);
            pane.setOnApiKeyCommitted(ignored -> {
                pane.saveApiKeyOnly(config);
                persistSecrets.run();
            });
            llmPanes.put(v, pane);
        }

        StackPane providerPane = new StackPane();
        providerPane.setAlignment(Pos.TOP_LEFT);
        providerPane.getChildren().addAll(baiduPane, youdaoPane);
        providerPane.getChildren().addAll(llmPanes.values());

        Runnable refreshVendor = () -> {
            ProviderVendor selected = vendorBox.getValue();
            showOnly(baiduPane, selected == ProviderVendor.BAIDU);
            showOnly(youdaoPane, selected == ProviderVendor.YOUDAO);
            for (Map.Entry<ProviderVendor, LlmConfigPane> e : llmPanes.entrySet()) {
                showOnly(e.getValue(), e.getKey() == selected);
            }
        };
        vendorBox.valueProperty().addListener((obs, o, n) -> refreshVendor.run());
        refreshVendor.run();

        ComboBox<String> sourceLangBox = new ComboBox<>();
        sourceLangBox.getItems().addAll(
                "auto", "en", "zh", "jp", "kor", "fra", "de", "spa", "ru", "pt", "it");
        sourceLangBox.setValue(config.getSourceLang());
        sourceLangBox.setMaxWidth(Double.MAX_VALUE);
        sourceLangBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String code) {
                return LanguageUtils.displayName(code);
            }

            @Override
            public String fromString(String label) {
                return label;
            }
        });

        Spinner<Integer> intervalSpinner = new Spinner<>(0, 60000, config.getRequestIntervalMs());
        intervalSpinner.setEditable(true);
        intervalSpinner.setPrefWidth(160);

        Label vendorLabel = new Label("提供商：");
        Label sourceLangLabel = new Label("源语言：");
        Label intervalLabel = new Label("请求间隔：");

        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(12);
        header.add(vendorLabel, 0, 0);
        header.add(vendorBox, 1, 0);
        GridPane.setHgrow(vendorBox, Priority.ALWAYS);

        GridPane common = new GridPane();
        common.setHgap(10);
        common.setVgap(12);
        common.add(sourceLangLabel, 0, 0);
        common.add(sourceLangBox, 1, 0);
        common.add(intervalLabel, 0, 1);
        HBox intervalBox = new HBox(8, intervalSpinner, new Label("毫秒"));
        intervalBox.setAlignment(Pos.CENTER_LEFT);
        common.add(intervalBox, 1, 1);
        GridPane.setHgrow(sourceLangBox, Priority.ALWAYS);

        VBox root = new VBox(12, header, providerPane, new Separator(), common);
        root.setPadding(new Insets(16));

        getDialogPane().setContent(root);
        getDialogPane().setPrefWidth(620);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        SkinManager.getInstance().applyTo(getDialogPane());

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                ProviderVendor selected = vendorBox.getValue();
                if (selected == ProviderVendor.BAIDU) {
                    baiduPane.save(config);
                } else if (selected == ProviderVendor.YOUDAO) {
                    youdaoPane.save(config);
                } else if (selected != null && selected.isLlm()) {
                    llmPanes.get(selected).save(config);
                }
                config.setSourceLang(sourceLangBox.getValue());
                config.setRequestIntervalMs(intervalSpinner.getValue());
                state.saveConfig();
                state.getTranslationService().refreshProvider();
            }
            return null;
        });
    }

    private static void showOnly(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
