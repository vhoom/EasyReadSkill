package com.song.config;

import com.llm.api.LlmVendor;
import com.song.model.ProviderVendor;

/**
 * 各大模型厂商配置集合（持久化到 config.json）。
 */
public class LlmConfigs {

    private LlmSlotConfig deepseek = new LlmSlotConfig();
    private LlmSlotConfig zhipu = new LlmSlotConfig();
    private LlmSlotConfig gemini = new LlmSlotConfig();
    private LlmSlotConfig kimi = new LlmSlotConfig();
    private LlmSlotConfig openai = new LlmSlotConfig();
    private LlmSlotConfig qianwen = new LlmSlotConfig();

    public LlmSlotConfig getDeepseek() {
        if (deepseek == null) deepseek = new LlmSlotConfig();
        return deepseek;
    }
    public void setDeepseek(LlmSlotConfig deepseek) { this.deepseek = deepseek; }

    public LlmSlotConfig getZhipu() {
        if (zhipu == null) zhipu = new LlmSlotConfig();
        return zhipu;
    }
    public void setZhipu(LlmSlotConfig zhipu) { this.zhipu = zhipu; }

    public LlmSlotConfig getGemini() {
        if (gemini == null) gemini = new LlmSlotConfig();
        return gemini;
    }
    public void setGemini(LlmSlotConfig gemini) { this.gemini = gemini; }

    public LlmSlotConfig getKimi() {
        if (kimi == null) kimi = new LlmSlotConfig();
        return kimi;
    }
    public void setKimi(LlmSlotConfig kimi) { this.kimi = kimi; }

    public LlmSlotConfig getOpenai() {
        if (openai == null) openai = new LlmSlotConfig();
        return openai;
    }
    public void setOpenai(LlmSlotConfig openai) { this.openai = openai; }

    public LlmSlotConfig getQianwen() {
        if (qianwen == null) qianwen = new LlmSlotConfig();
        return qianwen;
    }
    public void setQianwen(LlmSlotConfig qianwen) { this.qianwen = qianwen; }

    /**
     * 按 UI 厂商取槽位。
     *
     * @param vendor UI 厂商
     * @return 配置槽；非 LLM 返回 null
     */
    public LlmSlotConfig slot(ProviderVendor vendor) {
        if (vendor == null || !vendor.isLlm()) return null;
        return switch (vendor) {
            case DEEPSEEK -> getDeepseek();
            case ZHIPU -> getZhipu();
            case GEMINI -> getGemini();
            case KIMI -> getKimi();
            case OPENAI -> getOpenai();
            case QIANWEN -> getQianwen();
            default -> null;
        };
    }

    /**
     * UI 厂商 → {@link LlmVendor}。
     *
     * @param vendor UI 厂商
     * @return LLM 厂商
     */
    public static LlmVendor toLlmVendor(ProviderVendor vendor) {
        return switch (vendor) {
            case DEEPSEEK -> LlmVendor.DEEPSEEK;
            case ZHIPU -> LlmVendor.ZHIPU;
            case GEMINI -> LlmVendor.GEMINI;
            case KIMI -> LlmVendor.KIMI;
            case OPENAI -> LlmVendor.OPENAI;
            case QIANWEN -> LlmVendor.QIANWEN;
            default -> throw new IllegalArgumentException("非 LLM 厂商: " + vendor);
        };
    }

    /** 启动时重置各槽提示词。 */
    public void resetPrompts() {
        getDeepseek().resetPromptToDefault();
        getZhipu().resetPromptToDefault();
        getGemini().resetPromptToDefault();
        getKimi().resetPromptToDefault();
        getOpenai().resetPromptToDefault();
        getQianwen().resetPromptToDefault();
    }
}
