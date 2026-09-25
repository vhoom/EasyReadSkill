package com.translator.bootstrap;

import com.translator.baidu.BaiduConfig;
import com.translator.facade.TranslateService;
import com.translator.youdao.YoudaoConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranslatorBootstrapTest {

    @Test
    void allowsYoudaoOnly() {
        TranslateService s = TranslatorBootstrap.create(
                new YoudaoConfig("key", "secret"), null);
        assertNotNull(s);
    }

    @Test
    void allowsBaiduOnly() {
        TranslateService s = TranslatorBootstrap.create(
                null, new BaiduConfig("id", "secret"));
        assertNotNull(s);
    }

    @Test
    void rejectsBothNull() {
        assertThrows(IllegalArgumentException.class,
                () -> TranslatorBootstrap.create(null, null));
    }
}
