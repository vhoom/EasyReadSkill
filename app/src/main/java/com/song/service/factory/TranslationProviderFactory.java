package com.song.service.factory;

import com.song.model.ProviderType;

/**
 * 根据配置创建翻译服务商实例（实现委托给 translator 模块）。
 */
public final class TranslationProviderFactory {

    private TranslationProviderFactory() {}

    public static TranslationProvider create(ProviderType type) {
        TranslatorHttpBridge.install();
        ProviderType providerType = type != null ? type : ProviderType.getDefault();
        return new TranslatorBackedProvider(providerType);
    }
}
