package com.llm.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmLanguagesTest {

    @Test
    void vendorCodesBecomeLanguageNames() {
        assertEquals("日本語", LlmLanguages.displayName("jp"));
        assertEquals("한국어", LlmLanguages.displayName("kor"));
        assertEquals("法语", LlmLanguages.displayName("fra"));
        assertEquals("简体中文", LlmLanguages.displayName("zh"));
        assertEquals("English", LlmLanguages.displayName("en"));
        assertEquals("繁體中文", LlmLanguages.displayName("zh-CHT"));
    }

    @Test
    void blankAndAutoBecomeAutoDetect() {
        assertEquals(LlmLanguages.AUTO, LlmLanguages.displayName(null));
        assertEquals(LlmLanguages.AUTO, LlmLanguages.displayName("  "));
        assertEquals(LlmLanguages.AUTO, LlmLanguages.displayName("auto"));
        assertEquals(LlmLanguages.AUTO, LlmLanguages.displayName("AUTO"));
    }

    @Test
    void unknownCodeIsReturnedAsIs() {
        assertEquals("xx-YY", LlmLanguages.displayName("xx-YY"));
    }
}
