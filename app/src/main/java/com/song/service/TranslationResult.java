package com.song.service;

/**
 * 翻译结果。
 */
public class TranslationResult {

    private final boolean success;
    private final boolean interrupted;
    private final boolean conflict;
    private final String text;
    private final String errorMessage;

    private TranslationResult(boolean success, boolean interrupted, boolean conflict,
                              String text, String errorMessage) {
        this.success = success;
        this.interrupted = interrupted;
        this.conflict = conflict;
        this.text = text;
        this.errorMessage = errorMessage;
    }

    public static TranslationResult success(String text) {
        return new TranslationResult(true, false, false, text, null);
    }

    public static TranslationResult failure(String errorMessage) {
        return new TranslationResult(false, false, false, null, errorMessage);
    }

    public static TranslationResult interrupted() {
        return new TranslationResult(false, true, false, null, "已中断");
    }

    /**
     * 文件当前描述与备份/上次写入不一致：需要用户决定按当前内容重新备份还是沿用旧备份。
     *
     * @param currentDescription 文件里现在读到的描述
     * @return 冲突结果
     */
    public static TranslationResult conflict(String currentDescription) {
        return new TranslationResult(false, false, true, currentDescription, "原文已变更");
    }

    public boolean isSuccess() { return success; }
    public boolean isInterrupted() { return interrupted; }
    public boolean isConflict() { return conflict; }
    public String getText() { return text; }
    public String getErrorMessage() { return errorMessage; }
}
