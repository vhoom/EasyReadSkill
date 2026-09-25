package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.TranslationResult;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 翻译服务厂商统一接口。
 */
public interface TranslationProvider {

    ProviderType getType();

    TranslationResult translate(String text, String from, String to, AppConfig config);

    /**
     * 单次请求的文本长度上限（超过则分段）。
     * 百度/有道 NMT 的 query 很短，大模型的上下文窗口大得多。
     *
     * @return 建议的分段阈值（字符数）
     */
    default int maxChunkLength() {
        return 1800;
    }

    /**
     * @return 一次请求最多拼几段原文；&lt;=1 表示不支持批量拼接
     */
    default int maxBatchItems() {
        return 1;
    }

    /**
     * @return 一次批量请求的原文总长度上限（字符）；&lt;=0 表示不限制
     */
    default int maxBatchChars() {
        return 0;
    }

    /**
     * 批量翻译。默认逐段调用 {@link #translate}，任一段失败即抛出，
     * 由调用方决定是否回退（回退后各段互不影响）。
     *
     * @param texts  待翻译文本（等长返回）
     * @param from   源语言
     * @param to     目标语言
     * @param config 应用配置
     * @return 译文列表
     */
    default List<String> translateBatch(List<String> texts, String from, String to,
                                        AppConfig config) {
        List<String> out = new ArrayList<>(texts.size());
        for (String text : texts) {
            TranslationResult result = translate(text, from, to, config);
            if (result.isInterrupted()) {
                throw new IllegalStateException("已中断", new InterruptedIOException("已中断"));
            }
            if (!result.isSuccess()) {
                throw new IllegalStateException(result.getErrorMessage());
            }
            out.add(result.getText());
        }
        return out;
    }
}
