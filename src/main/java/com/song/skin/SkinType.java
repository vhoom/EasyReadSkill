package com.song.skin;


public enum SkinType {
    LIGHT("浅色"),
    DARK("深色");

    private static final String FONT_FAMILY =
            "Inter, Segoe UI, Microsoft YaHei, PingFang SC, sans-serif";
    private static final int FONT_SIZE = 14;
    private static final int RADIUS = 8;
    private static final int SPACING = 8;

    private final String label;

    SkinType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDarkChrome() {
        return this == DARK;
    }

    /**
     * 读配置字符串。旧值 WHITE → LIGHT；BLACK / HIGH_CONTRAST → DARK。
     */
    public static SkinType fromConfig(String raw) {
        if (raw == null || raw.isBlank()) return LIGHT;
        return switch (raw.trim().toUpperCase()) {
            case "DARK", "BLACK", "HIGH_CONTRAST" -> DARK;
            default -> LIGHT;
        };
    }

    SkinTokens tokens() {
        return switch (this) {
            case LIGHT -> new SkinTokens(
                    /* background    canvas              */ "#FAF9F5",
                    /* surface       canvas              */ "#FAF9F5",
                    /* surfaceAlt    surface-card        */ "#EFE9DE",
                    /* text          ink                 */ "#141413",
                    /* textSecondary muted               */ "#6C6A64",
                    /* border        hairline            */ "#E6DFD8",
                    /* primary       coral               */ "#CC785C",
                    /* primaryText   on-primary          */ "#FFFFFF",
                    /* primaryHover  coral stays         */ "#CC785C",
                    /* hover         surface-soft        */ "#F5F0E8",
                    /* selection     cream-strong        */ "#E8E0D2",
                    /* success       semantic            */ "#2F8A4E",
                    /* warning       semantic            */ "#8A6808",
                    /* danger        error               */ "#C64545",
                    /* focusRing     coral 15%           */ "#CC785C26",
                    /* mark          coral               */ "#CC785C",
                    /* scrollThumb   ink overlay         */ "rgba(20, 20, 19, 0.28)",
                    /* pressed       cream-strong        */ "#E8E0D2",
                    /* primaryPressed coral-active       */ "#A9583E",
                    /* strokeStrong  muted-soft          */ "#8E8B82",
                    /* focusStroke   coral               */ "#CC785C",
                    /* shadow        rare                */ "rgba(20, 20, 19, 0.08)",
                    FONT_FAMILY, FONT_SIZE, RADIUS, SPACING);
            case DARK -> new SkinTokens(
                    /* background    surface-dark        */ "#181715",
                    /* surface       dark-elevated       */ "#252320",
                    /* surfaceAlt    dark-soft           */ "#1F1E1B",
                    /* text          on-dark             */ "#FAF9F5",
                    /* textSecondary on-dark-soft        */ "#A09D96",
                    /* border        warm hairline       */ "#3A3834",
                    /* primary       coral               */ "#CC785C",
                    /* primaryText   on-primary          */ "#FFFFFF",
                    /* primaryHover  coral stays         */ "#CC785C",
                    /* hover         elevated lift       */ "#2E2C28",
                    /* selection     coral wash          */ "#3A2E28",
                    /* success       semantic            */ "#5DB872",
                    /* warning       accent-amber        */ "#E8A55A",
                    /* danger        lifted error        */ "#E07A72",
                    /* focusRing     coral 25%           */ "#CC785C40",
                    /* mark          coral               */ "#CC785C",
                    /* scrollThumb   cream overlay       */ "rgba(250, 249, 245, 0.28)",
                    /* pressed       darker elevated     */ "#2C2A27",
                    /* primaryPressed coral-active       */ "#A9583E",
                    /* strokeStrong  muted               */ "#6C6A64",
                    /* focusStroke   coral               */ "#CC785C",
                    /* shadow        rare                */ "rgba(0, 0, 0, 0.45)",
                    FONT_FAMILY, FONT_SIZE, RADIUS, SPACING);
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
