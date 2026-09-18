package com.song.ui;

import com.song.model.FilterType;
import com.song.model.SkillFile;
import com.song.model.TranslateStatus;
import com.song.skin.SkinManager;
import com.song.skin.SkinType;
import com.song.skin.control.AnimatedButton;
import com.song.skin.control.AnimatedComboBox;
import com.song.util.LanguageUtils;
import com.song.util.UiHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.Map;

public class TopBar extends VBox {
    private final AppState state;

    public TopBar(AppState state) {
        this.state = state;
        setPadding(new Insets(8));
        setSpacing(8);
        getStyleClass().add("top-bar");

        AnimatedButton pathBtn = new AnimatedButton("扫描路径管理");
        pathBtn.setOnAction(e -> new ScanPathDialog(state).showAndWait());

        AnimatedButton reloadBtn = new AnimatedButton("扫描文件");
        reloadBtn.setOnAction(e -> {
            state.reload();
            UiHelper.info("完成", "共扫描到 " + state.getAllFiles().size() + " 个 skill.md 文件。");
        });

        Label skinLabel = new Label("皮肤：");
        AnimatedComboBox<SkinType> skinBox = new AnimatedComboBox<>();
        skinBox.getItems().setAll(SkinType.values());
        skinBox.setValue(SkinManager.getInstance().getCurrentType());
        skinBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                SkinManager.getInstance().setSkinType(n);
                // 确保 Scene 已存在时立即重新应用样式
                if (getScene() != null) {
                    SkinManager.getInstance().applyTo(getScene());
                }
            }
        });



        Label apiLabel = new Label("API：" + apiText());
        AnimatedButton apiBtn = new AnimatedButton("API 配置");
        apiBtn.setOnAction(e -> {
            new ApiConfigDialog(state).showAndWait();
            apiLabel.setText("API：" + apiText());
        });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        HBox actionRow = new HBox(10,
                pathBtn, reloadBtn, actionSpacer,
                skinLabel, skinBox, apiLabel, apiBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("筛选：");
        ToggleGroup filterGroup = new ToggleGroup();
        HBox filterBox = new HBox(6);
        Map<FilterType, RadioButton> filterButtons = new EnumMap<>(FilterType.class);
        for (FilterType ft : FilterType.values()) {
            RadioButton rb = new RadioButton(ft.getLabel());
            rb.setToggleGroup(filterGroup);
            rb.setUserData(ft);
            if (ft == state.getFilter()) rb.setSelected(true);
            rb.selectedProperty().addListener((obs, o, n) -> {
                if (n) state.filterProperty().set(ft);
            });
            filterButtons.put(ft, rb);
            filterBox.getChildren().add(rb);
        }
        state.versionProperty().addListener((obs, o, n) -> updateFilterCounts(filterButtons));
        updateFilterCounts(filterButtons);

        Label langLabel = new Label("目标语言：");
        AnimatedComboBox<String> langBox = new AnimatedComboBox<>();
        langBox.getItems().addAll("zh", "en", "jp", "kor", "fra", "de", "spa", "ru");
        langBox.setValue(state.getConfig().getTargetLang());
        langBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String code) {
                return LanguageUtils.displayName(code);
            }

            @Override
            public String fromString(String label) {
                return label;
            }
        });
        langBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                state.getConfig().setTargetLang(n);
                state.saveConfig();
            }
        });

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        HBox filterRow = new HBox(10, filterLabel, filterBox, filterSpacer, langLabel, langBox);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(actionRow, filterRow);
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
            if (status == null) continue;
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