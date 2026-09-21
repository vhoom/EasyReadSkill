package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.HttpCalls;
import com.song.service.TranslationErrorMessages;
import com.song.service.TranslationResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.UUID;

/**
 * 有道文本翻译 API。
 */
public class YoudaoTextTranslationProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YoudaoTextTranslationProvider.class);
    private static final String API_URL = "https://openapi.youdao.com/api";

    @Override
    public ProviderType getType() { return ProviderType.YOUDAO_TEXT; }

    @Override
    public TranslationResult translate(String q, String from, String to, AppConfig config) {
        try {
            String appKey = config.getAppId();
            String secret = config.getSecretKey();
            String salt = UUID.randomUUID().toString();
            String curtime = String.valueOf(System.currentTimeMillis() / 1000L);
            String sign = sha256Hex(appKey + signInput(q) + salt + curtime + secret);

            StringBuilder form = new StringBuilder();
            addParam(form, "q", q);
            addParam(form, "from", YoudaoLanguageMapper.map(from));
            addParam(form, "to", YoudaoLanguageMapper.map(to));
            addParam(form, "appKey", appKey);
            addParam(form, "salt", salt);
            addParam(form, "sign", sign);
            addParam(form, "signType", "v3");
            addParam(form, "curtime", curtime);
            if (config.getYoudaoDomain() != null
                    && !config.getYoudaoDomain().isEmpty()
                    && !"general".equals(config.getYoudaoDomain())) {
                addParam(form, "domain", config.getYoudaoDomain());
            }

            HttpURLConnection conn = HttpCalls.open(API_URL);
            try {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(form.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    throw new IllegalStateException("HTTP " + code + " - " + API_URL);
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                if (json.has("errorCode") && !"0".equals(json.get("errorCode").getAsString())) {
                    String errCode = json.get("errorCode").getAsString();
                    throw new IllegalStateException(TranslationErrorMessages.youdao(errCode));
                }
                JsonArray translation = json.getAsJsonArray("translation");
                if (translation == null || translation.isEmpty()) {
                    throw new IllegalStateException("有道文本翻译返回结果为空");
                }
                StringBuilder translated = new StringBuilder();
                for (int i = 0; i < translation.size(); i++) {
                    if (translated.length() > 0) translated.append('\n');
                    translated.append(translation.get(i).getAsString());
                }
                return TranslationResult.success(translated.toString());
            } finally {
                HttpCalls.finish(conn);
            }
        } catch (Exception e) {
            if (HttpCalls.causedByCancel(e)) return TranslationResult.interrupted();
            LOG.error("有道文本翻译请求失败", e);
            return TranslationResult.failure(e.getMessage() == null
                    ? "有道文本翻译请求失败" : e.getMessage());
        }
    }

    private static void addParam(StringBuilder form, String key, String value) {
        if (form.length() > 0) form.append('&');
        form.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
    }

    private static String signInput(String text) {
        String value = text == null ? "" : text;
        int len = value.length();
        if (len <= 20) return value;
        return value.substring(0, 10) + len + value.substring(len - 10);
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : bytes) formatter.format("%02x", b);
                return formatter.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }
}