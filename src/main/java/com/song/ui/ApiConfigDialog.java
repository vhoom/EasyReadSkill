package com.song.ui;

import com.song.config.AppConfig;
import com.song.model.ProviderVendor;
import com.song.skin.SkinManager;
import com.song.skin.animation.DialogAnimator;
import com.song.skin.control.AnimatedComboBox;
import com.song.ui.config.BaiduConfigPane;
import com.song.ui.config.YoudaoConfigPane;
import com.song.util.LanguageUtils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * 顶部 API 配置入口。
 * 不同提供商使用独立配置面板。
 */
public class ApiConfigDialog extends Dialog<Void> {

    public ApiConfigDialog(AppState state) {
        AppConfig config = state.getConfig();

        setTitle("API 配置");
        setResizable(true);

        AnimatedComboBox<ProviderVendor> vendorBox = new AnimatedComboBox<>();
        vendorBox.getItems().setAll(ProviderVendor.values());
        vendorBox.setValue(config.getVendor());
        vendorBox.setMaxWidth(Double.MAX_VALUE);

        BaiduConfigPane baiduPane = new BaiduConfigPane();
        YoudaoConfigPane youdaoPane = new YoudaoConfigPane();
        baiduPane.load(config);
        youdaoPane.load(config);

        StackPane providerPane = new StackPane(baiduPane, youdaoPane);
        providerPane.setAlignment(Pos.TOP_LEFT);

        Runnable refreshVendor = () -> {
            boolean baidu = vendorBox.getValue() == ProviderVendor.BAIDU;
            baiduPane.setVisible(baidu);
            baiduPane.setManaged(baidu);
            youdaoPane.setVisible(!baidu);
            youdaoPane.setManaged(!baidu);
        };
        vendorBox.valueProperty().addListener((obs, o, n) -> refreshVendor.run());
        refreshVendor.run();

        AnimatedComboBox<String> sourceLangBox = new AnimatedComboBox<>();
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
        getDialogPane().setPrefWidth(580);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        SkinManager.getInstance().applyTo(getDialogPane());
        DialogAnimator.install(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (vendorBox.getValue() == ProviderVendor.BAIDU) {
                    baiduPane.save(config);
                } else {
                    youdaoPane.save(config);
                }
                config.setSourceLang(sourceLangBox.getValue());
                config.setRequestIntervalMs(intervalSpinner.getValue());
                state.saveConfig();
                state.getTranslationService().refreshProvider();
            }
            return null;
        });
    }
}