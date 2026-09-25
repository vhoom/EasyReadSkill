package com.llm.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmEndpointsTest {

    @Test
    void deepseekBaseGetsV1Once() {
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.DEEPSEEK, "https://api.deepseek.com"));
        assertEquals("https://api.deepseek.com/v1/models",
                LlmEndpoints.modelsUrl(LlmVendor.DEEPSEEK, "https://api.deepseek.com"));
    }

    @Test
    void pastedFullUrlIsNotDoubled() {
        // 用户把完整接口地址粘进 Base URL 的常见错误
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.DEEPSEEK,
                        "https://api.deepseek.com/v1/chat/completions"));
        assertEquals("https://api.deepseek.com/v1/models",
                LlmEndpoints.modelsUrl(LlmVendor.DEEPSEEK, "https://api.deepseek.com/v1/models"));
    }

    @Test
    void trailingSlashAndV1AreNormalized() {
        assertEquals("https://api.moonshot.cn/v1/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.KIMI, "https://api.moonshot.cn/v1/"));
    }

    @Test
    void vendorSpecificPathsStayUntouched() {
        assertEquals("https://open.bigmodel.cn/api/paas/v4/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.ZHIPU, "https://open.bigmodel.cn/api/paas/v4"));
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.GEMINI,
                        "https://generativelanguage.googleapis.com/v1beta/openai"));
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.QIANWEN,
                        "https://dashscope.aliyuncs.com/compatible-mode/v1"));
    }

    @Test
    void emptyBaseFallsBackToVendorDefault() {
        assertEquals(LlmVendor.OPENAI.defaultBaseUrl() + "/v1/chat/completions",
                LlmEndpoints.chatUrl(LlmVendor.OPENAI, ""));
        assertEquals(LlmVendor.OPENAI.defaultBaseUrl() + "/v1/models",
                LlmEndpoints.modelsUrl(LlmVendor.OPENAI, "  "));
    }
}
