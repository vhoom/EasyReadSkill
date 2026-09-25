package com.translator.baidu.internal;

import com.translator.api.TranslateResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaiduResponseTest {

    @Test
    void joinsMultipleTransResultsWithNewline() {
        String body = "{"
                + "\"from\":\"en\",\"to\":\"zh\","
                + "\"trans_result\":["
                + "{\"src\":\"a\",\"dst\":\"甲\"},"
                + "{\"src\":\"b\",\"dst\":\"乙\"}"
                + "]}";
        TranslateResponse r = BaiduResponse.parse(body);
        assertEquals("甲\n乙", r.getText());
        assertEquals("en", r.getFrom());
        assertEquals("zh", r.getTo());
    }

    @Test
    void throwsOnErrorCode() {
        String body = "{\"error_code\":\"54001\",\"error_msg\":\"Invalid Sign\"}";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BaiduResponse.parse(body));
        assertTrue(ex.getMessage().contains("54001"));
    }
}
