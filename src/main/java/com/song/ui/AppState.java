package com.song.ui;

import com.song.config.AppConfig;
import com.song.config.ConfigManager;
import com.song.model.*;

import com.song.model.*;
import com.song.service.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppState {
    private final AppConfig config;
    private final RecordManager recordManager;
    private final TranslationService translationService;
    private final SkillFileService fileService;

    private final ObservableList<SkillFile> allFiles = FXCollections.observableArrayList();
    private final ObjectProperty<SkillFile> selectedFile = new SimpleObjectProperty<>();
    private final ObjectProperty<FilterType> filter = new SimpleObjectProperty<>(FilterType.UNTRANSLATED);
    private final ObjectProperty<EffectType> effect = new SimpleObjectProperty<>(EffectType.OVERWRITE);
    private final IntegerProperty version = new SimpleIntegerProperty(0);

    public AppState() {
        this.config = ConfigManager.load();
        this.recordManager = new RecordManager();
        this.translationService = new TranslationService(config);
        this.fileService = new SkillFileService(recordManager, translationService);
    }

    public AppConfig getConfig() { return config; }
    public RecordManager getRecordManager() { return recordManager; }
    public TranslationService getTranslationService() { return translationService; }
    public SkillFileService getFileService() { return fileService; }

    public ObservableList<SkillFile> getAllFiles() { return allFiles; }

    public ObjectProperty<SkillFile> selectedFileProperty() { return selectedFile; }
    public SkillFile getSelectedFile() { return selectedFile.get(); }
    public void setSelectedFile(SkillFile f) { selectedFile.set(f); }

    public ObjectProperty<FilterType> filterProperty() { return filter; }
    public FilterType getFilter() { return filter.get(); }

    public ObjectProperty<EffectType> effectProperty() { return effect; }
    public EffectType getEffect() { return effect.get(); }

    public IntegerProperty versionProperty() { return version; }
    public void bumpVersion() { version.set(version.get() + 1); }

    /** 从数据库记录加载已保存的 skill 列表，不扫描文件系统。 */
    public void loadFromRecords() {
        allFiles.clear();
        for (TranslationRecord rec : recordManager.all().values()) {
            if (rec == null || rec.getPath() == null) continue;
            File file = new File(rec.getPath());
            if (!file.isFile()) continue;

            String parentName = file.getParentFile() == null ? "" : file.getParentFile().getName();
            SkillFile sf = new SkillFile(rec.getPath(), parentName);
            sf.setOriginalDescription(rec.getOriginalDescription());
            sf.setTranslatedDescription(rec.getTranslatedDescription());
            sf.setStatus(rec.getStatus() != null ? rec.getStatus() : TranslateStatus.UNTRANSLATED);
            allFiles.add(sf);
        }
        bumpVersion();
    }

    /** 重新扫描所有已启用路径，并把新的 skill 批量写入数据库。 */
    public void reload() {
        allFiles.clear();
        List<SkillFile> found = FileScanner.scan(config.getScanPaths());

        Map<String, String> backups = new ConcurrentHashMap<>();

        // 并行读取文件，减少大量 skill.md 的扫描等待时间。
        found.parallelStream().forEach(sf -> {
            try {
                String content = SkillFileService.readFile(sf.getFilePath());
                String desc = SkillParser.extractDescription(content);
                sf.setOriginalDescription(desc);
                if (desc == null) {
                    sf.setStatus(TranslateStatus.FAILED);
                } else {
                    backups.put(sf.getFilePath(), desc);
                }
            } catch (IOException e) {
                sf.setStatus(TranslateStatus.FAILED);
            }
        });

        recordManager.recordFirstBackupBatch(backups);

        for (SkillFile sf : found) {
            if (sf.getStatus() == TranslateStatus.FAILED) continue;
            TranslationRecord rec = recordManager.get(sf.getFilePath());
            if (rec == null) {
                sf.setStatus(TranslateStatus.UNTRANSLATED);
            } else {
                sf.setStatus(rec.getStatus() != null
                        ? rec.getStatus() : TranslateStatus.UNTRANSLATED);
                sf.setTranslatedDescription(rec.getTranslatedDescription());
            }
        }

        allFiles.addAll(found);
        bumpVersion();
    }
    public void saveConfig() { ConfigManager.save(config); }
}