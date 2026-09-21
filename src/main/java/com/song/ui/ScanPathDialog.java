package com.song.ui;

import com.song.model.ScanPath;
import com.song.skin.SkinManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScanPathDialog extends Dialog<Void> {

    private final AppState state;
    private final VBox listBox = new VBox(6);

    public ScanPathDialog(AppState state) {
        this.state = state;
        setTitle("扫描路径管理");
        setResizable(true);

        listBox.setPadding(new Insets(8));
        listBox.setMaxWidth(Double.MAX_VALUE);
        refresh();

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("添加路径");
        addBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择要扫描的目录");
            File f = dc.showDialog(getOwner());
            if (f != null) {
                state.getConfig().getScanPaths().add(new ScanPath(f.getAbsolutePath(), true));
                state.saveConfig();
                refresh();
            }
        });

        HBox bottom = new HBox(8, addBtn);
        bottom.setPadding(new Insets(8));

        VBox root = new VBox(sp, bottom);
        VBox.setVgrow(sp, Priority.ALWAYS);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getDialogPane().setContent(root);
        getDialogPane().setPrefSize(580, 380);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        SkinManager.getInstance().applyTo(getDialogPane());
    }

    private void refresh() {
        listBox.getChildren().clear();
        List<ScanPath> paths = new ArrayList<>(state.getConfig().getScanPaths());
        if (paths.isEmpty()) {
            listBox.getChildren().add(new Label("（暂无扫描路径，点击下方添加）"));
            return;
        }
        paths.sort(ScanPathDialog::comparePaths);
        for (int i = 0; i < paths.size(); i++) {
            ScanPath sp = paths.get(i);
            if (i > 0 && paths.get(i - 1).isEnabled() && !sp.isEnabled()) {
                listBox.getChildren().add(selectionDivider());
            }
            listBox.getChildren().add(buildRow(sp));
        }
    }

    /** 已启用在前，未启用在后；各组按路径不区分大小写升序。 */
    private static int comparePaths(ScanPath a, ScanPath b) {
        if (a.isEnabled() != b.isEnabled()) return a.isEnabled() ? -1 : 1;
        String pathA = a.getPath() == null ? "" : a.getPath();
        String pathB = b.getPath() == null ? "" : b.getPath();
        return pathA.compareToIgnoreCase(pathB);
    }

    private static Region selectionDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("selection-divider");
        divider.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(divider, new Insets(4, 4, 4, 4));
        return divider;
    }

    private HBox buildRow(ScanPath sp) {
        CheckBox cb = new CheckBox();
        cb.setSelected(sp.isEnabled());
        cb.selectedProperty().addListener((obs, o, n) -> {
            sp.setEnabled(n);
            state.saveConfig();
            Platform.runLater(this::refresh);
        });

        Label pathLabel = new Label(sp.getPath());
        pathLabel.getStyleClass().add("secondary");
        HBox.setHgrow(pathLabel, Priority.ALWAYS);
        pathLabel.setMaxWidth(Double.MAX_VALUE);

        Button delBtn = new Button("删除");
        delBtn.setOnAction(e -> {
            state.getConfig().getScanPaths().remove(sp);
            state.saveConfig();
            refresh();
        });

        HBox row = new HBox(8, cb, pathLabel, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }
}