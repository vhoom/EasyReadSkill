package com.song.skin;

public class DarkSkinStrategy extends AbstractSkinStrategy {
    @Override
    protected SkinTokens createPalette() {
        return new SkinTokens(
                "#111827",
                "#1F2937",
                "#273449",
                "#F9FAFB",
                "#9CA3AF",
                "#374151",
                "#60A5FA",
                "#111827",
                "#374151",
                "#1E3A8A",
                "#4ADE80",
                "#FBBF24",
                "#F87171",
                "rgba(0, 0, 0, 0.35)",
                FONT_FAMILY, FONT_SIZE, RADIUS, SPACING
        );
    }
}