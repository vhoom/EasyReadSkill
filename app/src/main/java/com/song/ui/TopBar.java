package com.song.ui;

import com.song.model.FilterType;
import com.song.model.SkillFile;
import com.song.model.TranslateStatus;
import com.song.skin.SkinManager;
import com.song.skin.SkinType;
import com.song.service.NetworkWorker;
import com.song.util.LanguageUtils;
import com.song.util.UiHelper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class TopBar extends VBox {
    private final AppState state;

    public TopBar(AppState state) {
        this.state = state;
        setPadding(new Insets(12, 16, 12, 16));
        getStyleClass().add("top-bar");

        Button pathBtn = new Button("扫描路径");
        pathBtn.setOnAction(e -> new ScanPathDialog(state).showAndWait());

        Button reloadBtn = new Button("扫描文件");
        reloadBtn.setOnAction(e -> {
            reloadBtn.setDisable(true);
            NetworkWorker.submit(() -> {
                try {
                    var found = state.scanFiles();
                    Platform.runLater(() -> {
                        state.applyScannedFiles(found);
                        reloadBtn.setDisable(false);
                        UiHelper.info("完成", "共扫描到 " + found.size() + " 个 SKILL.md 文件。");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        reloadBtn.setDisable(false);
                        UiHelper.error("扫描失败", ex.getMessage() == null ? "未知错误" : ex.getMessage());
                    });
                }
            });
        });

        Label skinLabel = new Label("外观");
        skinLabel.getStyleClass().add("secondary");
        ComboBox<SkinType> skinBox = new ComboBox<>();
        skinBox.getItems().setAll(SkinType.values());
        skinBox.setValue(SkinManager.getInstance().getCurrentType());
        skinBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                SkinManager.getInstance().setSkinType(n);
                state.getConfig().setSkinType(n.name());
                state.saveConfig();
                if (getScene() != null) {
                    SkinManager.getInstance().applyTo(getScene());
                }
            }
        });

        Label apiLabel = new Label(apiText());
        apiLabel.getStyleClass().add("secondary");
        Button apiBtn = new Button("API 配置");
        ComboBox<String> sourceBox = langBox(state.getConfig().getSourceLang(), code -> {
            state.getConfig().setSourceLang(code);
            state.saveConfig();
        }, "auto", "en", "zh", "jp", "kor", "fra", "de", "spa", "ru", "pt", "it");
        ComboBox<String> targetBox = langBox(state.getConfig().getTargetLang(), code -> {
            state.getConfig().setTargetLang(code);
            state.saveConfig();
        }, "zh", "en", "jp", "kor", "fra", "de", "spa", "ru");
        apiBtn.setOnAction(e -> {
            new ApiConfigDialog(state).showAndWait();
            apiLabel.setText(apiText());
            sourceBox.setValue(state.getConfig().getSourceLang());
        });

        HBox filterBox = new HBox(12);
        Map<FilterType, RadioButton> filterButtons = new EnumMap<>(FilterType.class);
        ToggleGroup filterGroup = new ToggleGroup();
        for (FilterType ft : FilterType.values()) {
            RadioButton rb = new RadioButton(ft.getLabel());
            rb.getStyleClass().add("filter-tab");
            rb.setToggleGroup(filterGroup);
            rb.setUserData(ft);
            if (ft == state.getFilter())
                rb.setSelected(true);
            rb.selectedProperty().addListener((obs, o, n) -> {
                if (n)
                    state.filterProperty().set(ft);
            });
            filterButtons.put(ft, rb);
            filterBox.getChildren().add(rb);
        }
        state.versionProperty().addListener((obs, o, n) -> updateFilterCounts(filterButtons));
        updateFilterCounts(filterButtons);

        Label langLabel = new Label("目标");
        langLabel.getStyleClass().add("secondary");
        Label sourceLabel = new Label("源语言");
        sourceLabel.getStyleClass().add("secondary");

        // 逻辑分组各自成一个 HBox，放进 FlowPane；窗口变窄时自动换到下一行。
        HBox scans = new HBox(16, pathBtn, reloadBtn);
        scans.setAlignment(Pos.CENTER_LEFT);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        HBox skinGroup = new HBox(8, skinLabel, skinBox);
        skinGroup.setAlignment(Pos.CENTER_LEFT);
        HBox apiGroup = new HBox(8, apiLabel, apiBtn);
        apiGroup.setAlignment(Pos.CENTER_LEFT);
        HBox sourceGroup = new HBox(8, sourceLabel, sourceBox);
        sourceGroup.setAlignment(Pos.CENTER_LEFT);
        HBox targetGroup = new HBox(8, langLabel, targetBox);
        targetGroup.setAlignment(Pos.CENTER_LEFT);

        FlowPane flow = new FlowPane(scans, filterBox, skinGroup, apiGroup, sourceGroup, targetGroup);
        flow.setHgap(16);
        flow.setVgap(12);
        flow.setAlignment(Pos.CENTER_LEFT);
        flow.setRowValignment(VPos.CENTER);

        getChildren().add(flow);
    }

    private ComboBox<String> langBox(String current, Consumer<String> save, String... codes) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(codes);
        box.setValue(current);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(String code) {
                return LanguageUtils.displayName(code);
            }

            @Override
            public String fromString(String label) {
                return label;
            }
        });
        box.valueProperty().addListener((obs, o, n) -> {
            if (n != null)
                save.accept(n);
        });
        return box;
    }

    private String apiText() {
        return state.getConfig().getVendor().getDisplayName();
    }

    private void updateFilterCounts(Map<FilterType, RadioButton> buttons) {
        int all = state.getAllFiles().size();
        int untranslated = 0;
        int translated = 0;
        int failed = 0;

        for (SkillFile sf : state.getAllFiles()) {
            TranslateStatus status = sf.getStatus();
            if (status == null)
                continue;
            switch (status) {
                case UNTRANSLATED -> untranslated++;
                case TRANSLATED -> translated++;
                case FAILED -> failed++;
            }
        }

        setFilterCount(buttons, FilterType.ALL, all);
        setFilterCount(buttons, FilterType.UNTRANSLATED, untranslated);
        setFilterCount(buttons, FilterType.TRANSLATED, translated);
        setFilterCount(buttons, FilterType.FAILED, failed);
    }

    private void setFilterCount(Map<FilterType, RadioButton> buttons,
            FilterType type, int count) {
        RadioButton button = buttons.get(type);
        if (button != null) {
            button.setText(type.getLabel() + " (" + count + ")");
        }
    }
}