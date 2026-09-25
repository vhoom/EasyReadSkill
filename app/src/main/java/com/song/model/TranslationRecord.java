package com.song.model;

public class TranslationRecord {
    private String path;
    private String originalDescription;      // 首次备份
    private String translatedDescription;    // 最近一次译文
    private TranslateStatus status;
    private EffectType effect;
    private long firstBackupTimestamp;
    private long lastTranslateTimestamp;
    private String previousOriginalDescription;   // 重新备份前的旧备份，用于回滚
    private long previousOriginalTimestamp;
    private String originalHash;                  // 原始备份的内容指纹
    private String translatedHash;                // 最近译文的内容指纹
    private boolean originalUnknown;               // 标记为已翻译但不知道原文
    private String originalSourcePath;             // 备份是从哪条记录继承/采用的

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

    public String getPreviousOriginalDescription() { return previousOriginalDescription; }
    public void setPreviousOriginalDescription(String previousOriginalDescription) {
        this.previousOriginalDescription = previousOriginalDescription;
    }

    public long getPreviousOriginalTimestamp() { return previousOriginalTimestamp; }
    public void setPreviousOriginalTimestamp(long t) { this.previousOriginalTimestamp = t; }

    public String getOriginalHash() { return originalHash; }
    public void setOriginalHash(String originalHash) { this.originalHash = originalHash; }

    public String getTranslatedHash() { return translatedHash; }
    public void setTranslatedHash(String translatedHash) { this.translatedHash = translatedHash; }

    /** @return true 表示这是"标记为已翻译"产生的记录，原文未知 */
    public boolean isOriginalUnknown() { return originalUnknown; }
    public void setOriginalUnknown(boolean originalUnknown) { this.originalUnknown = originalUnknown; }

    /** @return 备份来源路径，可能为空 */
    public String getOriginalSourcePath() { return originalSourcePath; }
    public void setOriginalSourcePath(String originalSourcePath) {
        this.originalSourcePath = originalSourcePath;
    }
}
