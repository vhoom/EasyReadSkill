package com.song.skin;

/**
 * AppSkin 构建器（Builder 模式）。
 */
public class SkinBuilder {

    private SkinType type;
    private SkinTokens tokens;

    public SkinBuilder type(SkinType type) {
        this.type = type;
        return this;
    }

    public SkinBuilder tokens(SkinTokens tokens) {
        this.tokens = tokens;
        return this;
    }

    public AppSkin build() {
        if (type == null) type = SkinType.LIGHT;
        if (tokens == null) tokens = new LightSkinStrategy().createTokens();
        String css = new CssBuilder(tokens).build();
        return new AppSkin(type, tokens, css);
    }
}