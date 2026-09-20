package com.song.ui;

import com.song.model.ScanPath;
import com.song.skin.SkinManager;
import com.song.skin.animation.DialogAnimator;
import com.song.skin.control.AnimatedButton;
import com.song.skin.control.AnimatedCheckBox;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
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

        AnimatedButton addBtn = new AnimatedButton("添加路径");
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
        DialogAnimator.install(this);
    }

    private void refresh() {
        listBox.getChildren().clear();
        List<ScanPath> paths = state.getConfig().getScanPaths();
        if (paths.isEmpty()) {
            listBox.getChildren().add(new Label("（暂无扫描路径，点击下方添加）"));
            return;
        }
        for (ScanPath sp : paths) {
            listBox.getChildren().add(buildRow(sp));
        }
    }

    private HBox buildRow(ScanPath sp) {
        AnimatedCheckBox cb = new AnimatedCheckBox();
        cb.setSelected(sp.isEnabled());
        cb.selectedProperty().addListener((obs, o, n) -> {
            sp.setEnabled(n);
            state.saveConfig();
        });

        Label pathLabel = new Label(sp.getPath());
        pathLabel.setStyle("-fx-font-size: 12;");
        HBox.setHgrow(pathLabel, Priority.ALWAYS);
        pathLabel.setMaxWidth(Double.MAX_VALUE);

        AnimatedButton delBtn = new AnimatedButton("删除");
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