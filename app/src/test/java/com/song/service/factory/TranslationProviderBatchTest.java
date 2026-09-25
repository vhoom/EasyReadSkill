package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.TranslationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranslationProviderBatchTest {

    /** 假 provider：把每段加上前缀，用来验证默认的"逐段批量"实现。 */
    private static final class FakeProvider implements TranslationProvider {
        private final boolean fail;

        FakeProvider(boolean fail) {
            this.fail = fail;
        }

        @Override
        public ProviderType getType() {
            return ProviderType.BAIDU_GENERAL;
        }

        @Override
        public TranslationResult translate(String text, String from, String to, AppConfig config) {
            if (fail) {
                return TranslationResult.failure("boom");
            }
            return TranslationResult.success("[" + text + "]");
        }
    }

    @Test
    void defaultBatchLoopsOverSingleTranslate() {
        List<String> out = new FakeProvider(false)
                .translateBatch(List.of("a", "b"), "en", "zh", new AppConfig());
        assertEquals(List.of("[a]", "[b]"), out);
    }

    @Test
    void defaultBatchThrowsSoCallerCanFallBack() {
        assertThrows(IllegalStateException.class, () -> new FakeProvider(true)
                .translateBatch(List.of("a"), "en", "zh", new AppConfig()));
    }

    @Test
    void onlyLlmProvidersAllowBatching() {
        TranslatorBackedProvider llm = new TranslatorBackedProvider(ProviderType.DEEPSEEK_CHAT);
        assertEquals(5, llm.maxBatchItems());
        assertEquals(6000, llm.maxBatchChars());
        assertEquals(8000, llm.maxChunkLength());

        TranslatorBackedProvider nmt = new TranslatorBackedProvider(ProviderType.BAIDU_GENERAL);
        assertEquals(1, nmt.maxBatchItems());
        assertEquals(0, nmt.maxBatchChars());
        assertEquals(1800, nmt.maxChunkLength());
    }
}
