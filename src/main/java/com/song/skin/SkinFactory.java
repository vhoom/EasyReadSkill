package com.song.skin;

/**
 * 皮肤工厂（Factory Method / Simple Factory）。
 */
public interface SkinFactory {
    AppSkin create(SkinType type);
}