package com.song.ui;

import com.song.config.AppConfig;
import com.song.config.ConfigManager;
import com.song.model.*;
import com.song.skin.SkinManager;
import com.song.skin.SkinType;
import com.song.service.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppState {

    private static final Logger LOG = LoggerFactory.getLogger(AppState.class);

    /** 列表统一排序：路径不区分大小写升序，保证启动与扫描后顺序一致。 */
    private static final Comparator<SkillFile> BY_PATH = Comparator.comparing(
            sf -> sf.getFilePath() == null ? "" : sf.getFilePath().toLowerCase());

    private final AppConfig config;
    private final RecordManager recordManager;
    private final TranslationService translationService;
    private final SkillFileService fileService;

    private final ObservableList<SkillFile> allFiles = FXCollections.observableArrayList();
    private final ObjectProperty<SkillFile> selectedFile = new SimpleObjectProperty<>();
    private final ObjectProperty<FilterType> filter = new SimpleObjectProperty<>(FilterType.UNTRANSLATED);
    private final ObjectProperty<EffectType> effect = new SimpleObjectProperty<>(EffectType.OVERWRITE);
    private final IntegerProperty version = new SimpleIntegerProperty(0);
    private final IntegerProperty selectedCount = new SimpleIntegerProperty(0);

    /** 界面上频繁变化的配置项合并写盘，避免点一次列表就重写一次文件。 */
    private final PauseTransition saveDelay = new PauseTransition(Duration.millis(600));

    public AppState() {
        this.config = ConfigManager.load();
        this.recordManager = new RecordManager();
        boolean archivedBefore = config.isRecordsArchived();
        DataMaintenance.archiveOrphanRecordsOnce(config, recordManager);
        if (!archivedBefore && config.isRecordsArchived()) {
            saveConfig();
        }
        // 老记录补内容指纹：有了它才能识别"同一段 description 出现在多个目录"
        if (recordManager.fillMissingHashes() > 0) {
            recordManager.flush();
        }
        this.translationService = new TranslationService(config);
        this.fileService = new SkillFileService(recordManager, translationService);
        this.saveDelay.setOnFinished(e -> saveConfig());
        restoreUiState();
    }

    private void restoreUiState() {
        try {
            filter.set(FilterType.valueOf(config.getLastFilter()));
        } catch (Exception ignored) {
            filter.set(FilterType.UNTRANSLATED);
        }
        try {
            effect.set(EffectType.valueOf(config.getLastEffect()));
        } catch (Exception ignored) {
            effect.set(EffectType.OVERWRITE);
        }
        try {
            SkinManager.getInstance().setSkinType(SkinType.fromConfig(config.getSkinType()));
        } catch (Exception ignored) {
            SkinManager.getInstance().setSkinType(SkinType.LIGHT);
        }

        filter.addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                config.setLastFilter(newValue.name());
                scheduleSaveConfig();
            }
        });
        effect.addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                config.setLastEffect(newValue.name());
                scheduleSaveConfig();
            }
        });
        selectedFile.addListener((obs, oldValue, newValue) -> {
            config.setLastSelectedPath(newValue == null ? "" : newValue.getFilePath());
            scheduleSaveConfig();
        });
    }

    /** 保存窗口状态。 */
    public void saveUiState(Stage stage) {
        if (stage == null) return;
        config.setWindowMaximized(stage.isMaximized());
        if (!stage.isMaximized()) {
            config.setWindowWidth(stage.getWidth());
            config.setWindowHeight(stage.getHeight());
            config.setWindowX(stage.getX());
            config.setWindowY(stage.getY());
        }
        recordManager.flush();
        saveConfig();
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

    /** @return 已勾选数量（右侧按钮标题与左侧提示共用） */
    public IntegerProperty selectedCountProperty() { return selectedCount; }

    /** 重新统计勾选数量（勾选变化后调用）。 */
    public void refreshSelectedCount() {
        int count = 0;
        for (SkillFile sf : allFiles) {
            if (sf.isSelected()) count++;
        }
        selectedCount.set(count);
    }

    /** 从记录加载已保存的 skill 列表，不扫描文件系统。 */
    public void loadFromRecords() {
        List<SkillFile> list = new ArrayList<>();
        for (TranslationRecord rec : recordManager.all().values()) {
            if (rec == null || rec.getPath() == null) continue;
            File file = new File(rec.getPath());
            if (!FileScanner.isOutermostSkillFile(file)) continue;

            String parentName = file.getParentFile() == null ? "" : file.getParentFile().getName();
            SkillFile sf = new SkillFile(rec.getPath(), parentName);
            sf.setExternal(FileScanner.isExternalSkill(config.getScanPaths(), rec.getPath()));
            sf.setOriginalDescription(rec.getOriginalDescription());
            sf.setTranslatedDescription(rec.getTranslatedDescription());
            sf.setStatus(rec.getStatus() != null ? rec.getStatus() : TranslateStatus.UNTRANSLATED);
            list.add(sf);
        }
        list.sort(BY_PATH);
        allFiles.setAll(list);
        restoreLastSelection();
        refreshSelectedCount();
        bumpVersion();
    }

    /**
     * 后台核对每个文件的实际内容，纠正记录里过期的状态。
     * 只改状态，不动备份与译文。
     */
    public void startStatusVerification() {
        List<SkillFile> snapshot = List.copyOf(allFiles);
        if (snapshot.isEmpty()) return;

        NetworkWorker.submit(() -> {
            List<Object[]> changed = new ArrayList<>();
            for (SkillFile sf : snapshot) {
                TranslateStatus actual = detectStatus(sf.getFilePath(),
                        readDescription(sf.getFilePath()));
                if (actual != sf.getStatus()) {
                    changed.add(new Object[]{sf.getFilePath(), actual});
                }
            }
            if (changed.isEmpty()) return;
            Platform.runLater(() -> {
                for (Object[] item : changed) {
                    String path = (String) item[0];
                    TranslateStatus status = (TranslateStatus) item[1];
                    for (SkillFile sf : allFiles) {
                        if (path.equals(sf.getFilePath())) {
                            sf.setStatus(status);
                            break;
                        }
                    }
                    recordManager.updateStatus(path, status);
                }
                recordManager.flush();
                bumpVersion();
                LOG.info("启动核对：修正 {} 个文件的状态", changed.size());
            });
        });
    }

    /**
     * 扫描文件系统。只读取与核对状态，不写备份
     * （备份在真正要覆盖文件的那一刻才产生）。
     *
     * @return 扫描到的文件列表
     */
    public List<SkillFile> scanFiles() {
        List<SkillFile> found = FileScanner.scan(config.getScanPaths());
        found.parallelStream().forEach(sf -> {
            String desc = readDescription(sf.getFilePath());
            TranslationRecord rec = recordManager.get(sf.getFilePath());
            // 备份永远是记录里的 originalDescription，不是当前文件内容
            sf.setOriginalDescription(rec != null ? rec.getOriginalDescription() : desc);
            sf.setTranslatedDescription(rec != null ? rec.getTranslatedDescription() : null);
            sf.setStatus(detectStatus(sf.getFilePath(), desc));
        });
        found.sort(BY_PATH);
        recordManager.flush();
        return found;
    }

    /** 必须在 JavaFX 线程调用。 */
    public void applyScannedFiles(List<SkillFile> found) {
        allFiles.setAll(found);
        restoreLastSelection();
        refreshSelectedCount();
        bumpVersion();
    }

    private void restoreLastSelection() {
        String path = config.getLastSelectedPath();
        if (path == null || path.isEmpty()) return;
        for (SkillFile sf : allFiles) {
            if (path.equals(sf.getFilePath())) {
                selectedFile.set(sf);
                return;
            }
        }
    }

    /**
     * 依据文件当前内容判断真实状态，避免筛选计数与文件现状不符。
     *
     * @param filePath 文件路径
     * @param currentDescription 文件当前 description，null 表示读不到
     * @return 状态
     */
    private TranslateStatus detectStatus(String filePath, String currentDescription) {
        if (currentDescription == null) {
            return TranslateStatus.FAILED;
        }
        TranslationRecord rec = recordManager.get(filePath);
        if (rec == null) {
            // 同一技能分布在多个扫描目录：内容就是别处记录翻出来的译文时，
            // 按已翻译登记，并采用那条记录的原文备份（而不是把译文当原文）
            TranslationRecord adopted =
                    recordManager.adoptKnownTranslation(filePath, currentDescription);
            return adopted != null ? TranslateStatus.TRANSLATED : TranslateStatus.UNTRANSLATED;
        }
        String lastWritten = rec.getTranslatedDescription();
        if (lastWritten != null && !lastWritten.isEmpty()
                && lastWritten.equals(currentDescription)) {
            return TranslateStatus.TRANSLATED;
        }
        String backup = rec.getOriginalDescription();
        if (backup != null && !backup.isEmpty() && backup.equals(currentDescription)) {
            return TranslateStatus.UNTRANSLATED;
        }
        // 既不是我们写进去的、也不是备份：被外部改过，按未翻译处理（翻译时会提示冲突）
        return TranslateStatus.UNTRANSLATED;
    }

    private static String readDescription(String filePath) {
        try {
            return SkillParser.extractDescription(SkillFileService.readFile(filePath));
        } catch (IOException e) {
            return null;
        }
    }

    private void scheduleSaveConfig() {
        if (Platform.isFxApplicationThread()) {
            saveDelay.playFromStart();
        } else {
            saveConfig();
        }
    }

    public void saveConfig() { ConfigManager.save(config); }
}
