package com.song.model;

import javafx.beans.property.*;

public class SkillFile {
    private final StringProperty filePath = new SimpleStringProperty();
    private final StringProperty parentName = new SimpleStringProperty();
    private final ObjectProperty<TranslateStatus> status =
            new SimpleObjectProperty<>(TranslateStatus.UNTRANSLATED);
    private final StringProperty originalDescription = new SimpleStringProperty();
    private final StringProperty translatedDescription = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public SkillFile() {}

    public SkillFile(String filePath, String parentName) {
        this.filePath.set(filePath);
        this.parentName.set(parentName);
    }

    public String getFilePath() { return filePath.get(); }
    public void setFilePath(String v) { filePath.set(v); }
    public StringProperty filePathProperty() { return filePath; }

    public String getParentName() { return parentName.get(); }
    public void setParentName(String v) { parentName.set(v); }
    public StringProperty parentNameProperty() { return parentName; }

    public TranslateStatus getStatus() { return status.get(); }
    public void setStatus(TranslateStatus v) { status.set(v); }
    public ObjectProperty<TranslateStatus> statusProperty() { return status; }

    public String getOriginalDescription() { return originalDescription.get(); }
    public void setOriginalDescription(String v) { originalDescription.set(v); }

    public String getTranslatedDescription() { return translatedDescription.get(); }
    public void setTranslatedDescription(String v) { translatedDescription.set(v); }

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean v) { selected.set(v); }
    public BooleanProperty selectedProperty() { return selected; }
}