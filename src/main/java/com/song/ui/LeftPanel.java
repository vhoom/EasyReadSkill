package com.song.ui;

import com.song.model.FilterType;
import com.song.model.SkillFile;
import com.song.model.TranslateStatus;
import com.song.skin.control.AnimatedCheckBox;
import com.song.skin.control.AnimatedListCell;
import com.song.util.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeftPanel extends VBox {

    private static final Logger LOG = LoggerFactory.getLogger(LeftPanel.class);

    private final AppState state;
    private final ListView<SkillFile> listView = new ListView<>();
    private final FilteredList<SkillFile> filtered;

    /** filePath -> 显示名 */
    private final Map<String, String> displayNames = new HashMap<>();
    /** filePath -> 小字路径（完整路径） */
    private final Map<String, String> displayPaths = new HashMap<>();

    public LeftPanel(AppState state) {
        this.state = state;

        Label title = new Label("skill.md 列表");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 6 8 6 8;");

        Button selectAllBtn = new Button("全选");
        Button invertBtn = new Button("反选");
        Button clearBtn = new Button("取消选择");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(6, title, headerSpacer, selectAllBtn, invertBtn, clearBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 6, 4, 6));

        filtered = new FilteredList<>(state.getAllFiles(), f -> true);
        listView.setItems(filtered);
        listView.setCellFactory(lv -> new SkillCell());

        selectAllBtn.setOnAction(e -> {
            for (SkillFile sf : new ArrayList<>(filtered)) sf.setSelected(true);
            listView.refresh();
        });
        invertBtn.setOnAction(e -> {
            for (SkillFile sf : new ArrayList<>(filtered)) sf.setSelected(!sf.isSelected());
            listView.refresh();
        });
        clearBtn.setOnAction(e -> {
            for (SkillFile sf : new ArrayList<>(filtered)) sf.setSelected(false);
            listView.refresh();
        });

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, n) -> state.setSelectedFile(n));

        // 双击列表项，打开文件所在目录并选中该文件
        listView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                SkillFile sf = listView.getSelectionModel().getSelectedItem();
                if (sf != null) openContainingDirectory(sf.getFilePath());
            }
        });

        state.filterProperty().addListener((obs, o, n) -> applyFilter());
        state.versionProperty().addListener((obs, o, n) -> {
            recomputeDisplay();
            applyFilter();
            listView.refresh();
        });

        VBox.setVgrow(listView, Priority.ALWAYS);
        getChildren().addAll(header, listView);
        setPrefWidth(380);
        getStyleClass().add("left-panel");

        recomputeDisplay();
        applyFilter();
    }

    private void openContainingDirectory(String filePath) {
        File file = new File(filePath);
        File dir = file.getParentFile();
        if (dir == null || !dir.isDirectory()) {
            UiHelper.warn("提示", "找不到文件所在目录：\n" + filePath);
            return;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", "/select," + file.getAbsolutePath()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("xdg-open", dir.getAbsolutePath()).start();
            }
        } catch (IOException e) {
            LOG.error("打开目录失败: {}", dir.getAbsolutePath(), e);
            UiHelper.warn("失败", "无法打开目录：\n" + dir.getAbsolutePath());
        }
    }

    private void applyFilter() {
        FilterType ft = state.getFilter();
        filtered.setPredicate(sf -> {
            if (sf == null) return false;
            switch (ft) {
                case ALL: return true;
                case UNTRANSLATED: return sf.getStatus() == TranslateStatus.UNTRANSLATED;
                case TRANSLATED: return sf.getStatus() == TranslateStatus.TRANSLATED;
                case FAILED: return sf.getStatus() == TranslateStatus.FAILED;
            }
            return true;
        });
    }

    // ========== 显示计算 ==========

    private void recomputeDisplay() {
        displayNames.clear();
        displayPaths.clear();

        for (SkillFile sf : state.getAllFiles()) {
            String abs = sf.getFilePath().replace('\\', '/');
            displayPaths.put(sf.getFilePath(), abs.replace('/', File.separatorChar));

            // 从直接父目录往上，找名字为 "skills" 的祖先
            String skillsAncestor = null;
            File p = new File(sf.getFilePath()).getParentFile();
            while (p != null) {
                if (p.getName().equalsIgnoreCase("skills")) {
                    skillsAncestor = p.getAbsolutePath().replace('\\', '/');
                    break;
                }
                p = p.getParentFile();
            }

            if (skillsAncestor != null) {
                // 从 skills 这一级开始，相对路径
                String rel = abs.substring(skillsAncestor.length());
                while (rel.startsWith("/")) rel = rel.substring(1);
                displayNames.put(sf.getFilePath(),
//                        "skills/" + rel.replace('/', File.separatorChar));
                        rel.replace('/', File.separatorChar));
            } else {
                // 找不到 skills 祖先，退回：父目录名/SKILL.md
                displayNames.put(sf.getFilePath(),
                        sf.getParentName() + File.separatorChar + "SKILL.md");
            }
        }
    }

    // ========== Cell ==========

    private class SkillCell extends AnimatedListCell<SkillFile> {
        private final AnimatedCheckBox cb = new AnimatedCheckBox();
        private final Label nameLabel = new Label();
        private final Label pathLabel = new Label();
        private final VBox box = new VBox(2);
        private Tooltip tooltip;
        private boolean updating = false;
        private SkillFile bound;

        SkillCell() {
            nameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
            pathLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

            HBox line = new HBox(8, cb, nameLabel);
            line.setAlignment(Pos.CENTER_LEFT);

            box.getChildren().addAll(line, pathLabel);
            box.setPadding(new Insets(4, 6, 4, 6));

            cb.selectedProperty().addListener((obs, o, n) -> {
                if (updating) return;
                if (bound != null) bound.setSelected(n);
            });
        }

        @Override
        protected void updateItem(SkillFile sf, boolean empty) {
            super.updateItem(sf, empty);
            if (empty || sf == null) {
                bound = null;
                setGraphic(null);
                setTooltip(null);
                return;
            }
            bound = sf;
            updating = true;
            cb.setSelected(sf.isSelected());

            String name = displayNames.get(sf.getFilePath());
            nameLabel.setText(name != null ? name : sf.getParentName());

            String path = displayPaths.get(sf.getFilePath());
            pathLabel.setText(path != null ? path : sf.getFilePath());

            // 悬浮显示完整路径
            if (tooltip == null) {
                tooltip = new Tooltip();
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(600);
                tooltip.setShowDelay(javafx.util.Duration.millis(150));
            }
            tooltip.setText(sf.getFilePath());
            setTooltip(tooltip);

            switch (sf.getStatus()) {
                case UNTRANSLATED: nameLabel.setTextFill(Color.web("#333333")); break;
                case TRANSLATED: nameLabel.setTextFill(Color.web("#2e7d32")); break;
                case FAILED: nameLabel.setTextFill(Color.web("#c62828")); break;
            }
            updating = false;
            setGraphic(box);
        }
    }
}