package com.song.util;

/**
 * 密钥展示：首 3 + 中间 * + 尾 4，并标注总长度。
 */
public final class SecretMask {

    private SecretMask() {}

    /**
     * @param secret 明文密钥
     * @return 如 {@code sk-****abcd（28）}；空串返回空
     */
    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        int n = secret.length();
        if (n <= 7) {
            return "*".repeat(n) + "（" + n + "）";
        }
        String head = secret.substring(0, 3);
        String tail = secret.substring(n - 4);
        return head + "*".repeat(n - 7) + tail + "（" + n + "）";
    }

    /**
     * 判断文本是否为掩码展示（含全角括号长度标注）。
     *
     * @param text 输入框文字
     * @return true 表示是掩码，不应当作新明文保存
     */
    public static boolean looksMasked(String text) {
        if (text == null || text.isBlank()) return false;
        return text.indexOf('（') > 0 && text.endsWith("）") && text.contains("*");
    }
}
