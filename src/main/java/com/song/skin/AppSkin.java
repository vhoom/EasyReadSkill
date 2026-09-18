package com.song.skin;

/**
 * 皮肤对象，承载皮肤类型、设计令牌和自动生成的 CSS。
 */
public class AppSkin {

    private final SkinType type;
    private final SkinTokens tokens;
    private final String css;

    AppSkin(SkinType type, SkinTokens tokens, String css) {
        this.type = type;
        this.tokens = tokens;
        this.css = css;
    }

    public SkinType getType() { return type; }
    public SkinTokens getTokens() { return tokens; }
    public String getCss() { return css; }
}