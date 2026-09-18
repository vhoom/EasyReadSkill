package com.song.skin;

public class LightSkinStrategy extends AbstractSkinStrategy {
    @Override
    protected SkinTokens createPalette() {
        return new SkinTokens(
                "#F7F8FA",
                "#FFFFFF",
                "#F1F5F9",
                "#1F2937",
                "#6B7280",
                "#E5E7EB",
                "#2563EB",
                "#FFFFFF",
                "#EFF6FF",
                "#DBEAFE",
                "#16A34A",
                "#D97706",
                "#DC2626",
                "rgba(15, 23, 42, 0.08)",
                FONT_FAMILY, FONT_SIZE, RADIUS, SPACING
        );
    }
}