package com.song.service;

/**
 * 翻译结果。
 */
public class TranslationResult {

    private final boolean success;
    private final boolean interrupted;
    private final String text;
    private final String errorMessage;

    private TranslationResult(boolean success, boolean interrupted, String text, String errorMessage) {
        this.success = success;
        this.interrupted = interrupted;
        this.text = text;
        this.errorMessage = errorMessage;
    }

    public static TranslationResult success(String text) {
        return new TranslationResult(true, false, text, null);
    }

    public static TranslationResult failure(String errorMessage) {
        return new TranslationResult(false, false, null, errorMessage);
    }

    public static TranslationResult interrupted() {
        return new TranslationResult(false, true, null, "已中断");
    }

    public boolean isSuccess() { return success; }
    public boolean isInterrupted() { return interrupted; }
    public String getText() { return text; }
    public String getErrorMessage() { return errorMessage; }
}