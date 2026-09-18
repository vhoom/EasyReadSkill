package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.TranslationResult;

/**
 * 翻译服务厂商统一接口。
 */
public interface TranslationProvider {

    ProviderType getType();

    TranslationResult translate(String text, String from, String to, AppConfig config);
}