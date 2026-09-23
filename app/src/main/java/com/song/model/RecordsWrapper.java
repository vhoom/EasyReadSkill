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

    public Map<String, TranslationRecord> getRecords() { return records; }
    public void setRecords(Map<String, TranslationRecord> records) {
        this.records = records;
    }

    public Map<String, String> getTranslationMemory() { return translationMemory; }
    public void setTranslationMemory(Map<String, String> translationMemory) {
        this.translationMemory = translationMemory;
    }
}