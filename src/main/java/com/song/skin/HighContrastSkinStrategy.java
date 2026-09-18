package com.song.skin;

public class HighContrastSkinStrategy extends AbstractSkinStrategy {
    @Override
    protected SkinTokens createPalette() {
        return new SkinTokens(
                "#000000",
                "#000000",
                "#111111",
                "#FFFFFF",
                "#FFFF00",
                "#FFFFFF",
                "#FFFF00",
                "#000000",
                "#333300",
                "#666600",
                "#00FF00",
                "#FFAA00",
                "#FF5555",
                "none",
                FONT_FAMILY, FONT_SIZE, 4, SPACING
        );
    }
}