package com.song.service;

/**
 * 翻译结果。
 */
public class TranslationResult {

    private final boolean success;
    private final String text;
    private final String errorMessage;

    private TranslationResult(boolean success, String text, String errorMessage) {
        this.success = success;
        this.text = text;
        this.errorMessage = errorMessage;
    }

    public static TranslationResult success(String text) {
        return new TranslationResult(true, text, null);
    }

    public static TranslationResult failure(String errorMessage) {
        return new TranslationResult(false, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getText() { return text; }
    public String getErrorMessage() { return errorMessage; }
}