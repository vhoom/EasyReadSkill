package com.song.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * description 的内容指纹。
 *
 * <p>用途：判断"两个目录下的 description 是不是同一段内容"。
 * 只做内容比对，不替代按路径记账——路径决定"这条记录属于哪个文件"，
 * 指纹只回答"这段文字是不是同一段"。</p>
 *
 * <p>规范化：统一换行（CRLF/CR → LF）并去掉首尾空白，避免纯排版差异导致判定不一致。
 * 空内容返回空串（而不是空内容的哈希），以便和"没有指纹"区分开。</p>
 */
public final class ContentHash {

    /** 取 SHA-256 前 16 字节，碰撞概率远低于本工具的数据规模。 */
    private static final int DIGEST_BYTES = 16;

    private ContentHash() {}

    /**
     * @param text 原文，可空
     * @return 规范化文本
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    /**
     * @param text 原文，可空
     * @return 32 位十六进制指纹；空内容返回空串
     */
    public static String of(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return "";
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
        byte[] bytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(DIGEST_BYTES * 2);
        for (int i = 0; i < DIGEST_BYTES && i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    /**
     * @param hash 指纹
     * @return 是否为空（没有指纹）
     */
    public static boolean isBlank(String hash) {
        return hash == null || hash.isEmpty();
    }

    /**
     * 两个文本是否相同（按规范化后的内容）。
     *
     * @param a 文本 a
     * @param b 文本 b
     * @return 是否相同
     */
    public static boolean sameText(String a, String b) {
        return normalize(a).equals(normalize(b));
    }
}
