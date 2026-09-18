package com.song.skin;

/**
 * 设计令牌（Design Tokens）。
 * 所有皮肤共享同一套语义化令牌，方便后续扩展主题。
 */
public record SkinTokens(
        String background,
        String surface,
        String surfaceAlt,
        String text,
        String textSecondary,
        String border,
        String primary,
        String primaryText,
        String hover,
        String selection,
        String success,
        String warning,
        String danger,
        String shadow,
        String fontFamily,
        int fontSize,
        int radius,
        int spacing
) {
}