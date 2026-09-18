package com.song.model;

public class TranslationRecord {
    private String path;
    private String originalDescription;      // 首次备份
    private String translatedDescription;    // 最近一次译文
    private TranslateStatus status;
    private EffectType effect;
    private long firstBackupTimestamp;
    private long lastTranslateTimestamp;

    public TranslationRecord() {}

    public TranslationRecord(String path, String originalDescription) {
        this.path = path;
        this.originalDescription = originalDescription;
        this.status = TranslateStatus.UNTRANSLATED;
        this.firstBackupTimestamp = System.currentTimeMillis();
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) {
        this.originalDescription = originalDescription;
    }

    public String getTranslatedDescription() { return translatedDescription; }
    public void setTranslatedDescription(String translatedDescription) {
        this.translatedDescription = translatedDescription;
    }

    public TranslateStatus getStatus() { return status; }
    public void setStatus(TranslateStatus status) { this.status = status; }

    public EffectType getEffect() { return effect; }
    public void setEffect(EffectType effect) { this.effect = effect; }

    public long getFirstBackupTimestamp() { return firstBackupTimestamp; }
    public void setFirstBackupTimestamp(long t) { this.firstBackupTimestamp = t; }

    public long getLastTranslateTimestamp() { return lastTranslateTimestamp; }
    public void setLastTranslateTimestamp(long t) { this.lastTranslateTimestamp = t; }
}