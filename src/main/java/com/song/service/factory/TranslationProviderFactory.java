package com.song.service.factory;

import com.song.model.ProviderType;

/**
 * 根据配置创建翻译服务商实例。
 */
public final class TranslationProviderFactory {

    private TranslationProviderFactory() {}

    public static TranslationProvider create(ProviderType type) {
        ProviderType providerType = type != null ? type : ProviderType.getDefault();
        return switch (providerType) {
            case BAIDU_FIELD -> new BaiduFieldTranslationProvider();
            case BAIDU_GENERAL -> new BaiduGeneralTranslationProvider();
            case BAIDU_LLM -> new BaiduLlmTranslationProvider();
            case YOUDAO_TEXT -> new YoudaoTextTranslationProvider();
            case YOUDAO_LLM -> new YoudaoTranslationProvider();
        };
    }
}