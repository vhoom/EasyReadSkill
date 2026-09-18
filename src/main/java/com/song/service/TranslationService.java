package com.song.service;

import com.song.config.AppConfig;
import com.song.service.factory.TranslationProvider;
import com.song.service.factory.TranslationProviderFactory;

/**
 * 翻译服务统一入口。
 */
public class TranslationService {

    private final AppConfig config;
    private volatile TranslationProvider provider;

    public TranslationService(AppConfig config) {
        this.config = config;
        refreshProvider();
    }

    public void refreshProvider() {
        this.provider = TranslationProviderFactory.create(config.getProvider());
    }

    public TranslationProvider getProvider() {
        return provider;
    }

    public TranslationResult translate(String q, String from, String to) {
        TranslationProvider current = provider;
        if (current == null) {
            refreshProvider();
            current = provider;
        }
        return current.translate(q, from, to, config);
    }
}