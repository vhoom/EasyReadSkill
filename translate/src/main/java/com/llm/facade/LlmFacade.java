package com.llm.facade;

import com.llm.api.LlmConfig;
import com.llm.api.LlmLanguages;
import com.llm.api.LlmPrompts;
import com.llm.api.LlmVendor;
import com.llm.http.OpenAiCompatClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM 对外门面：环境变量 → 模型列表 → chat / 翻译。
 * 请求经 OpenAI 兼容 HTTP（{@link java.net.http.HttpClient}）；与 {@code com.translator} 无依赖。
 */
public final class LlmFacade {

    private final LlmConfig config;
    private final OpenAiCompatClient client;
    private final List<String> models;

    /**
     * @param client HTTP 客户端
     * @param models 已拉取的模型列表（可空）
     */
    private LlmFacade(OpenAiCompatClient client, List<String> models) {
        this.client = client;
        this.config = client.config();
        this.models = Collections.unmodifiableList(new ArrayList<>(models));
    }

    /**
     * 用已有配置构造门面（不拉模型列表）。
     *
     * @param config 连接配置
     * @return 门面
     */
    public static LlmFacade of(LlmConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config 不能为空");
        }
        return new LlmFacade(new OpenAiCompatClient(config), List.of());
    }

    /**
     * 读环境变量，再拉模型列表。
     *
     * @param vendor 厂商
     * @return 已缓存模型列表的门面
     */
    public static LlmFacade connect(LlmVendor vendor) {
        LlmConfig cfg = LlmConfig.fromEnv(vendor);
        if (cfg == null) {
            throw new IllegalStateException(
                    vendor.displayName() + " 未配置 API Key，请设置 "
                            + String.join(" 或 ", vendor.apiKeyEnvs()));
        }
        OpenAiCompatClient client = new OpenAiCompatClient(cfg);
        List<String> models;
        try {
            models = client.listModels();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    vendor.displayName() + " 获取模型列表失败: " + e.getMessage(), e);
        }
        return new LlmFacade(client, models);
    }

    /**
     * 仅读环境变量，不请求网络；无 Key 返回 empty。
     *
     * @param vendor 厂商
     * @return 可选门面（models 为空）
     */
    public static Optional<LlmFacade> fromEnv(LlmVendor vendor) {
        LlmConfig cfg = LlmConfig.fromEnv(vendor);
        if (cfg == null) {
            return Optional.empty();
        }
        return Optional.of(new LlmFacade(new OpenAiCompatClient(cfg), List.of()));
    }

    /**
     * 对所有已配置 Key 的厂商执行 {@link #connect}；失败写入 skipped。
     *
     * @param skipped 失败说明收集器，可为 null
     * @return 成功连接的厂商映射
     */
    public static Map<LlmVendor, LlmFacade> connectAll(List<String> skipped) {
        Map<LlmVendor, LlmFacade> ok = new EnumMap<>(LlmVendor.class);
        for (LlmVendor v : LlmVendor.values()) {
            if (LlmConfig.fromEnv(v) == null) {
                continue;
            }
            try {
                ok.put(v, connect(v));
            } catch (RuntimeException e) {
                if (skipped != null) {
                    skipped.add(v.name() + ": " + e.getMessage());
                }
            }
        }
        return ok;
    }

    /** @return 厂商 */
    public LlmVendor vendor() { return config.vendor(); }

    /** @return 连接配置 */
    public LlmConfig config() { return config; }

    /** @return connect 时缓存的模型 id；fromEnv 未拉列表则为空 */
    public List<String> models() { return models; }

    /**
     * 重新拉取模型列表。
     *
     * @return 最新模型 id
     */
    public List<String> refreshModels() {
        return client.listModels();
    }

    /**
     * Chat Completions。
     *
     * @param model  模型；空则用配置默认
     * @param system 系统提示
     * @param user   用户消息
     * @return 回复文本
     */
    public String chat(String model, String system, String user) {
        return client.chat(model, system, user);
    }

    /**
     * 用默认翻译系统提示做一次 chat。
     *
     * @param user 用户消息
     * @return 回复文本
     */
    public String chat(String user) {
        return chat(null, LlmPrompts.DEFAULT_TRANSLATE_SYSTEM, user);
    }

    /**
     * 翻译（默认模型与系统提示）。
     *
     * @param text 原文
     * @param from 源语言
     * @param to   目标语言
     * @return 译文
     */
    public String translate(String text, String from, String to) {
        return translate(null, text, from, to, LlmPrompts.DEFAULT_TRANSLATE_SYSTEM);
    }

    /**
     * 翻译。
     *
     * @param model         模型；空则默认
     * @param text          原文
     * @param from          源语言
     * @param to            目标语言
     * @param systemPrompt  系统提示
     * @return 译文
     */
    public String translate(String model, String text, String from, String to, String systemPrompt) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to 不能为空");
        }
        // 大模型看不懂 jp / kor / fra 这类厂商码，提示词里必须写自然语言名
        String fromLabel = LlmLanguages.displayName(from);
        String toLabel = LlmLanguages.displayName(to);
        String user = "请将下列文本从「" + fromLabel + "」翻译为「" + toLabel + "」。\n\n" + text;
        String sys = (systemPrompt == null || systemPrompt.isBlank())
                ? LlmPrompts.DEFAULT_TRANSLATE_SYSTEM : systemPrompt;
        return chat(model, sys, user);
    }

    /**
     * 批量翻译：把多段文本拼进一次对话，再按标记拆回逐段译文。
     *
     * @param model        模型；空则默认
     * @param texts        待翻译文本（至少两段）
     * @param from         源语言
     * @param to           目标语言
     * @param userPrompt   用户自定义提示词；空则用默认
     * @return 与入参等长的译文列表
     * @throws IllegalStateException 拼装/拆解失败（调用方应回退到逐段翻译）
     */
    public List<String> translateBatch(String model, List<String> texts, String from, String to,
                                       String userPrompt) {
        if (!LlmBatch.packable(texts)) {
            throw new IllegalArgumentException("不满足批量翻译条件");
        }
        String fromLabel = LlmLanguages.displayName(from);
        String toLabel = LlmLanguages.displayName(to);
        String user = "请把下面 " + texts.size() + " 段文本分别从「" + fromLabel
                + "」翻译为「" + toLabel + "」。\n\n" + LlmBatch.pack(texts);
        String reply = chat(model, LlmPrompts.batchSystem(userPrompt), user);
        return LlmBatch.split(reply, texts.size());
    }
}
