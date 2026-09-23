package com.song.service;

import com.song.config.AppConfig;
import com.song.service.factory.TranslationProvider;
import com.song.service.factory.TranslationProviderFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 翻译服务统一入口。
 */
public class TranslationService {

    private final AppConfig config;
    private volatile TranslationProvider provider;
    private final AtomicLong nextRequestTime = new AtomicLong(0);

    public TranslationService(AppConfig config) {
        this.config = config;
        refreshProvider();
    }

    public void refreshProvider() {
        this.provider = TranslationProviderFactory.create(config.getProvider());
    }

    public int getRequestIntervalMs() { return config.getRequestIntervalMs(); }

    public TranslationProvider getProvider() {
        return provider;
    }

    public TranslationResult translate(String q, String from, String to) {
        if (HttpCalls.isCancelled()) {
            return TranslationResult.interrupted();
        }
        if (!waitForRateLimit()) {
            return TranslationResult.interrupted();
        }

        TranslationProvider current = provider;
        if (current == null) {
            refreshProvider();
            current = provider;
        }
        return current.translate(q, from, to, config);
    }

    /** 全局请求节流。被中断则返回 false，调用方不得继续发请求。 */
    private boolean waitForRateLimit() {
        int interval = config.getRequestIntervalMs();
        if (interval <= 0) return !HttpCalls.isCancelled();

        long now = System.currentTimeMillis();
        long next = nextRequestTime.updateAndGet(prev -> Math.max(prev, now) + interval);
        long wait = next - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !HttpCalls.isCancelled();
    }
}