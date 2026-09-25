package com.song.model;

import java.util.HashMap;
import java.util.Map;

public class RecordsWrapper {
    private Map<String, TranslationRecord> records = new HashMap<>();

    /**
     * 翻译记忆：key 为厂商/源语言/目标语言/原文，value 为已经得到的译文。
     * 用于避免同一原文在还原后再次翻译，或同一原文被多个文件重复翻译。
     */
    private Map<String, String> translationMemory = new HashMap<>();

    /**
     * 用户主动"清空备份数据"后压住自动采用的路径 → 当时的内容指纹。
     * 只要内容没变，就不再自动从别的记录采用原文备份；内容一变自动失效。
     */
    private Map<String, String> suppressedHashes = new HashMap<>();

    public Map<String, TranslationRecord> getRecords() { return records; }
    public void setRecords(Map<String, TranslationRecord> records) {
        this.records = records;
    }

    public Map<String, String> getTranslationMemory() { return translationMemory; }
    public void setTranslationMemory(Map<String, String> translationMemory) {
        this.translationMemory = translationMemory;
    }

    public Map<String, String> getSuppressedHashes() { return suppressedHashes; }
    public void setSuppressedHashes(Map<String, String> suppressedHashes) {
        this.suppressedHashes = suppressedHashes;
    }
}
