package com.song.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析和替换 skill.md 中的 Description 内容。
 *
 * <p>支持以下常见写法：</p>
 * <pre>
 * Description: plain text
 * description: plain text
 * description: "quoted text"
 * description: &gt;
 *   folded block text
 *   continued
 * description: |
 *   literal block line 1
 *   literal block line 2
 * </pre>
 */
public class SkillParser {

    /** 匹配 description/Description 键，允许大小写和行首缩进。 */
    private static final Pattern DESC_LINE_PATTERN =
            Pattern.compile("(?i)^([ \\t]*description[ \\t]*:[ \\t]*)(.*)$");

    /** 提取第一处 Description 的内容；找不到返回 null。 */
    public static String extractDescription(String content) {
        DescriptionMatch match = findDescription(content);
        return match != null ? match.value : null;
    }

    /** 把第一处 Description 的内容替换为 newDesc，其余部分原样保留。 */
    public static String replaceDescription(String content, String newDesc) {
        DescriptionMatch match = findDescription(content);
        if (match == null) return content;

        String replacement = buildReplacement(match.prefix, newDesc);
        return content.substring(0, match.start) + replacement + content.substring(match.end);
    }

    public static boolean hasDescription(String content) {
        return findDescription(content) != null;
    }

    private static DescriptionMatch findDescription(String content) {
        if (content == null || content.isEmpty()) return null;

        int pos = 0;
        // Skip UTF-8 BOM if present; keep it in the untouched prefix.
        if (content.charAt(0) == '\uFEFF') pos = 1;

        while (pos < content.length()) {
            LineRef line = lineAt(content, pos);
            if (line == null) break;

            Matcher matcher = DESC_LINE_PATTERN.matcher(line.text);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String inline = matcher.group(2).trim();
                int keyIndent = leadingWhitespace(line.text);

                String value;
                int end = line.end;

                if (isBlockIndicator(inline)) {
                    // YAML block scalar: description: >  或 description: |
                    BlockResult block = readIndentedBlock(content, line.endWith, keyIndent);
                    value = formatBlock(block.lines, inline.charAt(0) == '|');
                    if (block.end >= 0) end = block.end;
                } else if (inline.isEmpty()) {
                    // description: 后换行，再接缩进内容
                    BlockResult block = readIndentedBlock(content, line.endWith, keyIndent);
                    value = formatBlock(block.lines, false);
                    if (block.end >= 0) end = block.end;
                } else {
                    value = unquote(inline);

                    // 兼容 description: 第一行
                    //         后续缩进行
                    BlockResult block = readIndentedBlock(content, line.endWith, keyIndent);
                    if (block.end >= 0 && !block.lines.isEmpty()) {
                        String continuation = formatBlock(block.lines, false);
                        if (!continuation.isEmpty()) {
                            value = value + " " + continuation;
                        }
                        end = block.end;
                    }
                }

                return new DescriptionMatch(line.start, end, prefix, value);
            }

            pos = line.endWith;
        }
        return null;
    }

    /** 判断 YAML 块标量标记：&gt;、&gt;-、&gt;+、|、|-、|+，也兼容缩进数字。 */
    private static boolean isBlockIndicator(String value) {
        if (value == null || value.isEmpty()) return false;
        char first = value.charAt(0);
        if (first != '>' && first != '|') return false;
        return value.matches("[>|][0-9]?[+-]?");
    }

    /**
     * 读取 description 键后所有比 keyIndent 更缩进的行。
     * 空白行暂时保留，遇到同缩进或更少缩进的行停止。
     */
    private static BlockResult readIndentedBlock(String content, int pos, int keyIndent) {
        List<String> raw = new ArrayList<>();
        int cursor = pos;
        int lastNonBlankEnd = -1;

        while (cursor < content.length()) {
            LineRef line = lineAt(content, cursor);
            if (line == null) break;

            String text = line.text;
            if (text.trim().isEmpty()) {
                raw.add("");
                cursor = line.endWith;
                continue;
            }

            int indent = leadingWhitespace(text);
            if (indent <= keyIndent) break;

            raw.add(text);
            lastNonBlankEnd = line.end;
            cursor = line.endWith;
        }

        // 去掉块首尾的空行，避免把下一个 key 前的空行算进来。
        while (!raw.isEmpty() && raw.get(0).trim().isEmpty()) raw.remove(0);
        while (!raw.isEmpty() && raw.get(raw.size() - 1).trim().isEmpty()) {
            raw.remove(raw.size() - 1);
        }

        if (raw.isEmpty()) return new BlockResult(List.of(), -1);

        int blockIndent = Integer.MAX_VALUE;
        for (String text : raw) {
            if (!text.trim().isEmpty()) {
                blockIndent = Math.min(blockIndent, leadingWhitespace(text));
            }
        }

        List<String> lines = new ArrayList<>();
        for (String text : raw) {
            if (text.trim().isEmpty()) {
                lines.add("");
            } else {
                lines.add(stripIndent(text, blockIndent));
            }
        }
        return new BlockResult(lines, lastNonBlankEnd);
    }

    /**
     * 处理 | 字面块。
     * 很多 SKILL.md 的 description 只是排版换行，需要按标点重新拼接为句子。
     */
    private static String joinLiteralLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i) == null ? "" : lines.get(i).stripTrailing();
            if (line.trim().isEmpty()) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append('\n');
                }
                continue;
            }

            String trimmed = line.trim();
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append(' ');
            }
            sb.append(trimmed);

            String next = i + 1 < lines.size() ? lines.get(i + 1) : null;
            if (shouldBreakLine(trimmed, next)) {
                sb.append('\n');
            }
        }
        return trimTrailingNewlines(sb.toString());
    }

    private static boolean shouldBreakLine(String line, String next) {
        if (line == null || line.isEmpty()) return true;
        // 句子结束标点后断行，避免把下一句拼到上一句。
        if (line.matches(".*[.!?。！？;；:：][\"')\\]}]*$")) return true;
        if (next == null) return false;
        String n = next.trim();
        // Markdown 列表、标题、代码块等保留换行。
        return n.startsWith("- ") || n.startsWith("* ") || n.startsWith("#")
                || n.startsWith("```") || n.startsWith("|");
    }

    private static String trimTrailingNewlines(String text) {
        int end = text.length();
        while (end > 0) {
            char c = text.charAt(end - 1);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') end--;
            else break;
        }
        return text.substring(0, end);
    }

    /** 将块内容转换为最终字符串。literal 对应 |，否则对应 &gt; 的折行规则。 */
    private static String formatBlock(List<String> lines, boolean literal) {
        if (lines == null || lines.isEmpty()) return "";

        if (literal) {
            return joinLiteralLines(lines);
        }

        StringBuilder sb = new StringBuilder();
        boolean previousWasText = false;
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                previousWasText = false;
            } else {
                if (previousWasText) sb.append(' ');
                sb.append(line.trim());
                previousWasText = true;
            }
        }
        return sb.toString().trim();
    }

    /** 生成替换文本；若译文包含换行则使用 YAML 块标量。 */
    private static String buildReplacement(String prefix, String newDesc) {
        String value = newDesc == null ? "" : newDesc;
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');

        if (normalized.indexOf('\n') >= 0) {
            String keyIndent = leadingIndent(prefix);
            String continuationIndent = keyIndent + "  ";
            StringBuilder sb = new StringBuilder(prefix).append('|');
            String[] parts = normalized.split("\n", -1);
            for (String part : parts) {
                sb.append('\n').append(continuationIndent).append(part);
            }
            return sb.toString();
        }

        return prefix + value;
    }

    /** 返回字符串开头的空白缩进。 */
    private static String leadingIndent(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) i++;
        return text.substring(0, i);
    }

    /** 返回字符串开头的空白字符数量。 */
    private static int leadingWhitespace(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) i++;
        return i;
    }

    private static String stripIndent(String text, int indent) {
        int i = 0;
        while (i < text.length() && i < indent
                && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return text.substring(i);
    }

    /** 去除 YAML 单行字符串两端的引号，并处理常见转义。 */
    private static String unquote(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                String inner = s.substring(1, s.length() - 1);
                if (first == '"') {
                    inner = inner.replace("\\\"", "\"").replace("\\\\", "\\");
                } else {
                    inner = inner.replace("''", "'");
                }
                return inner;
            }
        }
        return s;
    }

    private static LineRef lineAt(String content, int pos) {
        if (pos >= content.length()) return null;

        int newline = content.indexOf('\n', pos);
        int endWith = newline >= 0 ? newline + 1 : content.length();
        int end = newline >= 0 ? newline : content.length();
        if (end > pos && content.charAt(end - 1) == '\r') end--;

        return new LineRef(pos, end, endWith, content.substring(pos, end));
    }

    private static final class DescriptionMatch {
        final int start;
        final int end;
        final String prefix;
        final String value;

        DescriptionMatch(int start, int end, String prefix, String value) {
            this.start = start;
            this.end = end;
            this.prefix = prefix;
            this.value = value;
        }
    }

    private static final class LineRef {
        final int start;
        final int end;
        final int endWith;
        final String text;

        LineRef(int start, int end, int endWith, String text) {
            this.start = start;
            this.end = end;
            this.endWith = endWith;
            this.text = text;
        }
    }

    private static final class BlockResult {
        final List<String> lines;
        final int end;

        BlockResult(List<String> lines, int end) {
            this.lines = lines;
            this.end = end;
        }
    }
}
