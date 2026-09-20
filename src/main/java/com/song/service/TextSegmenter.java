package com.song.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 按换行和标点切分长文本。
 * 避免翻译接口对长文本只返回部分译文。
 */
public final class TextSegmenter {

    private TextSegmenter() {}

    public static List<String> split(String text, int maxLen) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                result.add("");
                continue;
            }
            if (line.length() <= maxLen) {
                result.add(line);
                continue;
            }
            splitByPunctuation(line, maxLen, result);
        }
        return result;
    }

    private static void splitByPunctuation(String line, int maxLen, List<String> result) {
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            current.append(c);
            boolean punctuation = c == '.' || c == '!' || c == '?' || c == ';'
                    || c == '。' || c == '！' || c == '？' || c == '；' || c == '，'
                    || c == ',' || c == '：' || c == ':';
            if (punctuation && current.length() >= 80) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else if (current.length() >= maxLen) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
    }
}