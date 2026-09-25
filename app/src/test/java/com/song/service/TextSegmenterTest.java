package com.song.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSegmenterTest {

    @Test
    void emptyAndShortLinesStay() {
        assertTrue(TextSegmenter.split(null, 100).isEmpty());
        assertTrue(TextSegmenter.split("", 100).isEmpty());
        assertEquals(List.of("hello", "", "world"), TextSegmenter.split("hello\n\nworld", 100));
        assertEquals(List.of("hello", "", "world"), TextSegmenter.split("hello\r\n\r\nworld", 100));
    }

    @Test
    void splitsLongLineOnPunctuationThenHardLimit() {
        String sentence = "a".repeat(79) + ". " + "b".repeat(10);
        List<String> parts = TextSegmenter.split(sentence, 200);
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).endsWith("."));
        assertEquals("b".repeat(10), parts.get(1));

        String solid = "x".repeat(250);
        List<String> chunks = TextSegmenter.split(solid, 100);
        assertEquals(3, chunks.size());
        assertEquals(100, chunks.get(0).length());
        assertEquals(100, chunks.get(1).length());
        assertEquals(50, chunks.get(2).length());
    }
}
