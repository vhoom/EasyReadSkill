package com.translator.youdao.internal;

import com.translator.api.TranslateResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoudaoLlmResponseTest {

    @Test
    void prefersLatestTransFull() {
        String body = "data: {\"code\":\"0\",\"data\":{\"transFull\":\"Hi\",\"langType\":\"en\"},\"requestId\":\"r1\"}\n"
                + "data: {\"code\":\"0\",\"data\":{\"transFull\":\"Hi, world\",\"langType\":\"en\"},\"requestId\":\"r1\"}\n";
        TranslateResponse r = YoudaoLlmResponse.parse(body);
        assertEquals("Hi, world", r.getText());
        assertEquals("en", r.getLangType());
        assertEquals("r1", r.getRequestId());
    }

    @Test
    void throwsOnSseErrorCode() {
        String body = "data: {\"code\":\"400\",\"message\":\"'i'不能为空;\",\"successful\":false}\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> YoudaoLlmResponse.parse(body));
        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void throwsOnPlainJsonError() {
        String body = "{\"errorCode\":\"110\"}";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> YoudaoLlmResponse.parse(body));
        assertTrue(ex.getMessage().contains("110"));
    }
}
