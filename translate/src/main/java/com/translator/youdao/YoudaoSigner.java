package com.translator.youdao;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 有道签名算法，仅本包可见。无状态，线程安全。 */
final class YoudaoSigner {

    private final YoudaoConfig config;

    YoudaoSigner(YoudaoConfig config) {
        this.config = config;
    }

    String sign(String q, String salt, String curtime) {
        String input = truncate(q);
        String raw = config.appKey() + input + salt + curtime + config.appSecret();
        return sha256(raw);
    }

    private static String truncate(String q) {
        int len = q.length();
        if (len <= 20) {
            return q;
        }
        return q.substring(0, 10) + len + q.substring(len - 10);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 不可用", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 不可用", e);
        }
    }
}