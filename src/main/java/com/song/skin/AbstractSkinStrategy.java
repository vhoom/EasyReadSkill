package com.song.skin;

/**
 * 皮肤策略模板：统一字体、间距等基础令牌，具体颜色由子类实现。
 */
public abstract class AbstractSkinStrategy implements SkinStrategy {

    protected static final String FONT_FAMILY =
            "Microsoft YaHei, Segoe UI, PingFang SC, sans-serif";
    protected static final int FONT_SIZE = 13;
    protected static final int RADIUS = 8;
    protected static final int SPACING = 8;

    @Override
    public final SkinTokens createTokens() {
        return createPalette();
    }

    protected abstract SkinTokens createPalette();
}