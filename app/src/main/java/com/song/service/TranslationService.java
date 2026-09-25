package com.song.service;

import com.song.config.AppConfig;
import com.song.config.LlmConfigs;
import com.song.config.LlmSlotConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.service.factory.TranslationProvider;
import com.song.service.factory.TranslationProviderFactory;

import java.io.InterruptedIOException;
import java.util.List;
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

    /**
     * @return 单次请求的文本长度上限（超过则分段）
     */
    public int maxChunkLength() {
        TranslationProvider current = provider;
        return current == null ? 1800 : current.maxChunkLength();
    }

    /**
     * @return 一次请求最多拼几段原文；&lt;=1 表示不支持批量
     */
    public int maxBatchItems() {
        TranslationProvider current = provider;
        return current == null ? 1 : current.maxBatchItems();
    }

    /**
     * @return 一次批量请求的原文总长度上限（字符）；&lt;=0 表示不限制
     */
    public int maxBatchChars() {
        TranslationProvider current = provider;
        return current == null ? 0 : current.maxBatchChars();
    }

    /**
     * 批量翻译（几段原文拼成一次请求）。失败时抛异常，调用方回退到逐段。
     *
     * @param texts 待翻译文本
     * @param from  源语言
     * @param to    目标语言
     * @return 译文列表
     */
    public List<String> translateBatch(List<String> texts, String from, String to) {
        if (HttpCalls.isCancelled()) {
            throw new IllegalStateException("已中断", new InterruptedIOException("已中断"));
        }
        if (!waitForRateLimit()) {
            throw new IllegalStateException("已中断", new InterruptedIOException("已中断"));
        }
        TranslationProvider current = provider;
        if (current == null) {
            refreshProvider();
            current = provider;
        }
        return current.translateBatch(texts, from, to, config);
    }

    /**
     * 当前模型标识，用于翻译记忆键：换了模型就不再吃旧译文。
     *
     * @return 模型 id；非大模型返回空串
     */
    public String modelTag() {
        ProviderType type = config.getProvider();
        ProviderVendor vendor = type.getVendor();
        if (vendor == ProviderVendor.YOUDAO) {
            return type == ProviderType.YOUDAO_LLM ? String.valueOf(config.getHandleOption()) : "";
        }
        if (!vendor.isLlm()) {
            return "";
        }
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        return slot == null ? "" : slot.resolveModel(LlmConfigs.toLlmVendor(vendor));
    }

    /**
     * 当前提示词指纹，用于翻译记忆键：改了提示词就不再吃旧译文。
     *
     * @return 提示词哈希；无提示词返回 none
     */
    public String promptFingerprint() {
        String prompt = config.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            return "none";
        }
        return Integer.toHexString(prompt.hashCode());
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
