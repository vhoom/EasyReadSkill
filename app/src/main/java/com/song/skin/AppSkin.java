package com.song.skin;

/**
 * 皮肤对象：类型、设计令牌、生成的 CSS。
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

    public static AppSkin of(SkinType type) {
        SkinType t = type != null ? type : SkinType.LIGHT;
        SkinTokens tokens = t.tokens();
        return new AppSkin(t, tokens, new CssBuilder(tokens).build());
    }

    public SkinType getType() { return type; }
    public SkinTokens getTokens() { return tokens; }
    public String getCss() { return css; }
}
