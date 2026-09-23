package com.translator.baidu;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 百度签名算法，仅本包可见。无状态，线程安全。 */
final class BaiduSigner {

    private final BaiduConfig config;

    /** 构造 BaiduSigner。 */
    BaiduSigner(BaiduConfig config) {
        this.config = config;
    }

    /** 通用 / 大模型 */
    String sign(String q, String salt) {
        return md5(config.appId() + q + salt + config.secret());
    }

    /** 领域：多拼一个 domain */
    String signDomain(String q, String salt, String domain) {
        return md5(config.appId() + q + salt + domain + config.secret());
    }

    /** md5。 */
    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 不可用", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 不可用", e);
        }
    }
}