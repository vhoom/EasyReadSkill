package com.song.service.factory;

import com.song.service.TranslationErrorMessages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;

final class BaiduApiUtils {

    private BaiduApiUtils() {}

    static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : digest) formatter.format("%02x", b);
                return formatter.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    static String urlEncode(String text) {
        return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8);
    }

    /** 百度文档要求 q 中的空格使用 %20，而不是 +。 */
    static String urlEncodeQuery(String text) {
        return urlEncode(text).replace("+", "%20");
    }

    /**
     * 百度通用/领域翻译签名使用原始 q。
     * 注意：签名时 q 不做 URL encode，发送请求前再对 q 做 URL encode。
     */
    static String signText(String text) {
        return text == null ? "" : text;
    }

    static JsonObject postForm(String apiUrl, String form) throws IOException {
        HttpURLConnection conn = open(apiUrl);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        write(conn, form.getBytes(StandardCharsets.UTF_8));
        return readJson(conn);
    }

    static JsonObject postJson(String apiUrl, String json, String authorization) throws IOException {
        HttpURLConnection conn = open(apiUrl);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (authorization != null && !authorization.isEmpty()) {
            conn.setRequestProperty("Authorization", authorization);
        }
        write(conn, json.getBytes(StandardCharsets.UTF_8));
        return readJson(conn);
    }

    static String extractDst(JsonObject json) throws IOException {
        if (json.has("error_code")) {
            String code = json.get("error_code").getAsString();
            String msg = json.has("error_msg") ? json.get("error_msg").getAsString() : "";
            throw new IOException(TranslationErrorMessages.baidu(code)
                    + (msg == null || msg.isEmpty() ? "" : "（接口返回：" + msg + "）"));
        }
        JsonArray results = json.getAsJsonArray("trans_result");
        if (results == null || results.isEmpty()) {
            throw new IOException("百度翻译返回结果为空");
        }
        JsonObject first = results.get(0).getAsJsonObject();
        if (!first.has("dst")) {
            throw new IOException("百度翻译返回结果缺少 dst 字段");
        }
        return first.get("dst").getAsString();
    }

    private static HttpURLConnection open(String apiUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        return conn;
    }

    private static void write(HttpURLConnection conn, byte[] body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }
    }

    private static JsonObject readJson(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " - " + conn.getURL());
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }
}