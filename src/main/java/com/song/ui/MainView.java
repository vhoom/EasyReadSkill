package com.song.ui;

import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MainView extends BorderPane {
    public MainView(AppState state) {
        TopBar topBar = new TopBar(state);
        LeftPanel leftPanel = new LeftPanel(state);
        RightPanel rightPanel = new RightPanel(state);

        SplitPane split = new SplitPane(leftPanel, rightPanel);
        split.setDividerPositions(0.32);

        setTop(topBar);
        setCenter(split);

        state.loadFromRecords();
    }
}