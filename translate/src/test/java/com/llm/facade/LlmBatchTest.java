package com.llm.facade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmBatchTest {

    @Test
    void packAndSplitRoundTrip() {
        List<String> texts = List.of("First text.", "Second text.", "Third text.");
        String packed = LlmBatch.pack(texts);
        assertTrue(packed.contains("<<<EASEREAD#1>>>"));
        assertTrue(packed.contains("<<<EASEREAD#3>>>"));

        // 模拟模型回复：开头多一句解释，标记与译文交错
        String reply = "好的，以下是逐段翻译：\n"
                + "<<<EASEREAD#1>>>\n第一段译文。\n"
                + "<<<EASEREAD#2>>>\n第二段译文。\n"
                + "<<<EASEREAD#3>>>\n第三段译文。\n";

        assertEquals(List.of("第一段译文。", "第二段译文。", "第三段译文。"),
                LlmBatch.split(reply, 3));
    }

    @Test
    void splitKeepsInnerLineBreaksAndStripsCodeFence() {
        String reply = "```text\n"
                + "<<<EASEREAD#1>>>\nline A\n  line B\n"
                + "<<<EASEREAD#2>>>\n译文二\n"
                + "```\n";
        assertEquals(List.of("line A\n  line B", "译文二"), LlmBatch.split(reply, 2));
    }

    @Test
    void splitIgnoresStrayTrailingMarker() {
        String reply = "<<<EASEREAD#1>>>\n甲\n<<<EASEREAD#2>>>\n乙\n<<<EASEREAD#";
        assertEquals(List.of("甲", "乙"), LlmBatch.split(reply, 2));
    }

    @Test
    void splitRejectsBrokenReplies() {
        // 段数不符
        assertThrows(IllegalStateException.class,
                () -> LlmBatch.split("<<<EASEREAD#1>>>\n甲\n", 2));
        // 顺序错乱
        assertThrows(IllegalStateException.class,
                () -> LlmBatch.split("<<<EASEREAD#2>>>\n乙\n<<<EASEREAD#1>>>\n甲\n", 2));
        // 某段为空
        assertThrows(IllegalStateException.class,
                () -> LlmBatch.split("<<<EASEREAD#1>>>\n\n<<<EASEREAD#2>>>\n乙\n", 2));
        // 空回复
        assertThrows(IllegalStateException.class, () -> LlmBatch.split("   ", 2));
    }

    @Test
    void packableRequiresTwoCleanTexts() {
        assertFalse(LlmBatch.packable(List.of("only one")));
        assertFalse(LlmBatch.packable(List.of("a", "  ")));
        assertFalse(LlmBatch.packable(List.of("a", "带标记 <<<EASEREAD#9>>> 的原文")));
        assertTrue(LlmBatch.packable(List.of("a", "b")));
    }
}
