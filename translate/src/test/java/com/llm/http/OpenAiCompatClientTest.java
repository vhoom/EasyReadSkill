package com.llm.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatClientTest {

    @Test
    void errorMessageIsExtractedFromJson() {
        String body = "{\"error\":{\"message\":\"Model not found: deepseek-flash\","
                + "\"type\":\"invalid_request_error\"}}";
        assertEquals("HTTP 400（Model not found: deepseek-flash）",
                OpenAiCompatClient.describeError(400, body));
    }

    @Test
    void topLevelMessageIsUsed() {
        assertEquals("HTTP 401（Invalid API key）",
                OpenAiCompatClient.describeError(401, "{\"message\":\"Invalid API key\"}"));
    }

    @Test
    void longHtmlErrorIsTruncated() {
        String body = "<html>" + "x".repeat(1000) + "</html>";
        String described = OpenAiCompatClient.describeError(502, body);
        assertTrue(described.startsWith("HTTP 502（<html>"), described);
        assertTrue(described.endsWith("...）"), described);
        assertTrue(described.length() < 330, "描述过长: " + described.length());
    }

    @Test
    void emptyBodyStillReadable() {
        assertEquals("HTTP 500", OpenAiCompatClient.describeError(500, ""));
        assertEquals("HTTP 500", OpenAiCompatClient.describeError(500, null));
    }
}
