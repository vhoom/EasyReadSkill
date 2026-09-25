package com.llm.facade;

import com.llm.api.LlmPrompts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量翻译的拼装与拆解。
 *
 * <p>把多段原文用 {@code <<<EASEREAD#n>>>} 标记装进一次对话，再把回复按标记拆回，
 * 保证各段"拼在一起发、拆开来用"，互不干扰。任何异常（标记缺失、重复、顺序错乱、
 * 段内容为空、原文里本来就带标记）都抛出异常，由调用方回退到逐段翻译。</p>
 */
public final class LlmBatch {

    private static final Pattern MARKER = Pattern.compile(
            Pattern.quote(LlmPrompts.BATCH_MARKER_PREFIX)
                    + "(\\d+)"
                    + Pattern.quote(LlmPrompts.BATCH_MARKER_SUFFIX));

    private LlmBatch() {}

    /**
     * 是否满足批量条件：至少两段，且原文里不能出现标记本身。
     *
     * @param texts 待翻译文本
     * @return 能否批量
     */
    public static boolean packable(List<String> texts) {
        if (texts == null || texts.size() < 2) {
            return false;
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                return false;
            }
            if (text.contains(LlmPrompts.BATCH_MARKER_PREFIX)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把多段文本拼成一段带标记的请求体。
     *
     * @param texts 待翻译文本
     * @return 拼装结果
     */
    public static String pack(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(LlmPrompts.batchMarker(i + 1)).append('\n')
                    .append(texts.get(i).strip()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 按标记把回复拆回逐段译文。
     *
     * @param response 模型回复
     * @param expected 期望段数
     * @return 与入参等长的译文列表
     * @throws IllegalStateException 标记缺失、重复、顺序错乱或段内容为空
     */
    public static List<String> split(String response, int expected) {
        if (expected < 1) {
            throw new IllegalArgumentException("expected 必须 >= 1");
        }
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("批量翻译返回空内容");
        }
        String text = stripCodeFence(response);
        Matcher matcher = MARKER.matcher(text);

        List<Integer> indices = new ArrayList<>(expected);
        List<Integer> starts = new ArrayList<>(expected);
        List<Integer> ends = new ArrayList<>(expected);
        while (matcher.find()) {
            indices.add(Integer.parseInt(matcher.group(1)));
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        if (indices.size() != expected) {
            throw new IllegalStateException("批量翻译段标记数不符：期望 " + expected
                    + "，实际 " + indices.size());
        }

        List<String> out = new ArrayList<>(expected);
        for (int i = 0; i < expected; i++) {
            if (indices.get(i) != i + 1) {
                throw new IllegalStateException("批量翻译段标记顺序不符：" + indices);
            }
            int from = ends.get(i);
            int to = (i + 1 < expected) ? starts.get(i + 1) : text.length();
            String piece = text.substring(from, to).trim();
            // 末尾可能多出一个未闭合/残缺的标记，直接截断
            int stray = piece.indexOf(LlmPrompts.BATCH_MARKER_PREFIX);
            if (stray >= 0) {
                piece = piece.substring(0, stray).trim();
            }
            if (piece.isEmpty()) {
                throw new IllegalStateException("批量翻译第 " + (i + 1) + " 段为空");
            }
            out.add(piece);
        }
        return out;
    }

    /** 去掉模型自己套上的 ``` 代码块围栏。 */
    private static String stripCodeFence(String response) {
        String[] lines = response.replace("\r\n", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder(response.length());
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
