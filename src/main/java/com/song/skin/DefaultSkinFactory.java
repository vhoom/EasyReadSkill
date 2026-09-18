package com.song.skin;

import java.util.EnumMap;
import java.util.Map;

/**
 * 默认皮肤工厂：根据类型选择具体皮肤策略。
 * 使用 EnumMap 缓存已创建皮肤，避免重复构建（Flyweight 思想）。
 */
public class DefaultSkinFactory implements SkinFactory {

    private final Map<SkinType, AppSkin> cache = new EnumMap<>(SkinType.class);

    @Override
    public AppSkin create(SkinType type) {
        SkinType target = type != null ? type : SkinType.LIGHT;
        AppSkin cached = cache.get(target);
        if (cached != null) return cached;

        SkinStrategy strategy = switch (target) {
            case LIGHT -> new LightSkinStrategy();
            case DARK -> new DarkSkinStrategy();
            case HIGH_CONTRAST -> new HighContrastSkinStrategy();
        };
        SkinTokens tokens = strategy.createTokens();
        AppSkin skin = new SkinBuilder()
                .type(target)
                .tokens(tokens)
                .build();
        cache.put(target, skin);
        return skin;
    }
}