package com.song.service;

import com.song.config.ConfigManager;
import com.song.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.song.model.EffectType;
import com.song.model.RecordsWrapper;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class RecordManager {

    private static final Logger LOG = LoggerFactory.getLogger(RecordManager.class);

    private static final Path RECORDS_FILE =
            ConfigManager.getDataDir().resolve("records.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final RecordsWrapper wrapper;

    public RecordManager() {
        this.wrapper = loadFromDisk();
    }

    private RecordsWrapper loadFromDisk() {
        try {
            if (!Files.exists(RECORDS_FILE)) return new RecordsWrapper();
            String json = Files.readString(RECORDS_FILE, StandardCharsets.UTF_8);
            RecordsWrapper w = GSON.fromJson(json, RecordsWrapper.class);
            if (w == null) return new RecordsWrapper();
            if (w.getRecords() == null) w.setRecords(new HashMap<>());
            if (w.getTranslationMemory() == null) w.setTranslationMemory(new HashMap<>());
            return w;
        } catch (IOException e) {
            LOG.error("读取翻译记录失败: {}", RECORDS_FILE, e);
            return new RecordsWrapper();
        }
    }

    public synchronized void saveToDisk() {
        try {
            Files.createDirectories(ConfigManager.getDataDir());
            String json = GSON.toJson(wrapper);
            Path tmp = RECORDS_FILE.resolveSibling("records.json.tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, RECORDS_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("保存翻译记录失败: {}", RECORDS_FILE, e);
        }
    }

    public synchronized String getCachedTranslation(String key) {
        if (key == null) return null;
        return wrapper.getTranslationMemory().get(key);
    }

    public synchronized void putCachedTranslation(String key, String translatedText) {
        if (key == null || translatedText == null) return;
        wrapper.getTranslationMemory().put(key, translatedText);
        saveToDisk();
    }

    public TranslationRecord get(String path) {
        return wrapper.getRecords().get(path);
    }

    /**
     * 首次备份：仅当记录不存在或 originalDescription 为空时写入。
     * 已存在则原样返回，不覆盖原始备份。
     */
    public synchronized TranslationRecord recordFirstBackup(String path, String originalDescription) {
        Map<String, TranslationRecord> map = wrapper.getRecords();
        TranslationRecord rec = map.get(path);
        if (rec == null) {
            rec = new TranslationRecord(path, originalDescription);
            map.put(path, rec);
            saveToDisk();
        } else if (rec.getOriginalDescription() == null || rec.getOriginalDescription().isEmpty()) {
            rec.setOriginalDescription(originalDescription);
            rec.setFirstBackupTimestamp(System.currentTimeMillis());
            saveToDisk();
        }
        return rec;
    }

    /**
     * 批量首次备份：只在最后保存一次 records.json，减少文件 I/O。
     */
    public synchronized void recordFirstBackupBatch(Map<String, String> originals) {
        if (originals == null || originals.isEmpty()) return;
        Map<String, TranslationRecord> map = wrapper.getRecords();
        boolean changed = false;
        for (Map.Entry<String, String> entry : originals.entrySet()) {
            String path = entry.getKey();
            String original = entry.getValue();
            if (original == null) continue;

            TranslationRecord rec = map.get(path);
            if (rec == null) {
                rec = new TranslationRecord(path, original);
                map.put(path, rec);
                changed = true;
            } else if (rec.getOriginalDescription() == null
                    || rec.getOriginalDescription().isEmpty()) {
                rec.setOriginalDescription(original);
                rec.setFirstBackupTimestamp(System.currentTimeMillis());
                changed = true;
            }
        }
        if (changed) saveToDisk();
    }

    /**
     * 强制重新备份：覆盖已有 originalDescription。
     * 不修改 translatedDescription/status/effect，避免误改翻译状态。
     */
    public synchronized TranslationRecord rebackup(String path, String originalDescription) {
        Map<String, TranslationRecord> map = wrapper.getRecords();
        TranslationRecord rec = map.get(path);
        if (rec == null) {
            rec = new TranslationRecord(path, originalDescription);
            map.put(path, rec);
        } else {
            rec.setOriginalDescription(originalDescription);
            rec.setFirstBackupTimestamp(System.currentTimeMillis());
        }
        saveToDisk();
        return rec;
    }

    /** 记录一次翻译结果，不触碰 originalDescription */
    public synchronized void recordTranslation(String path, String translatedDescription,
                                               EffectType effect, TranslateStatus status) {
        TranslationRecord rec = wrapper.getRecords().get(path);
        if (rec == null) return;
        rec.setTranslatedDescription(translatedDescription);
        rec.setEffect(effect);
        rec.setStatus(status);
        rec.setLastTranslateTimestamp(System.currentTimeMillis());
        saveToDisk();
    }

    public synchronized void recordFailure(String path) {
        TranslationRecord rec = wrapper.getRecords().get(path);
        if (rec != null) {
            rec.setStatus(TranslateStatus.FAILED);
            rec.setLastTranslateTimestamp(System.currentTimeMillis());
            saveToDisk();
        }
    }

    public synchronized String getOriginalDescription(String path) {
        TranslationRecord rec = wrapper.getRecords().get(path);
        return rec != null ? rec.getOriginalDescription() : null;
    }

    public Map<String, TranslationRecord> all() {
        return wrapper.getRecords();
    }
}