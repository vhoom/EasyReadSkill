package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.TranslationErrorMessages;
import com.song.service.TranslationResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.UUID;

/**
 * 有道大模型翻译 API。
 */
public class YoudaoTranslationProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YoudaoTranslationProvider.class);
    private static final String API_URL = "https://openapi.youdao.com/proxy/http/llm-trans";

    @Override
    public ProviderType getType() { return ProviderType.YOUDAO_LLM; }

    @Override
    public TranslationResult translate(String q, String from, String to, AppConfig config) {
        try {
            String appKey = config.getAppId();
            String secret = config.getSecretKey();
            String salt = UUID.randomUUID().toString();
            String curtime = String.valueOf(System.currentTimeMillis() / 1000L);
            String sign = sha256Hex(appKey + signInput(q) + salt + curtime + secret);

            StringBuilder form = new StringBuilder();
            addParam(form, "appKey", appKey);
            addParam(form, "salt", salt);
            addParam(form, "signType", "v3");
            addParam(form, "sign", sign);
            addParam(form, "curtime", curtime);
            addParam(form, "i", q);
            addParam(form, "handleOption", config.getHandleOption());
            if (!config.getPrompt().isEmpty()) addParam(form, "prompt", config.getPrompt());
            addParam(form, "from", YoudaoLanguageMapper.map(from));
            addParam(form, "to", YoudaoLanguageMapper.map(to));
            addParam(form, "streamType", "full");

            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(form.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("HTTP " + code + " - " + API_URL);
            }

            String lastFull = null;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("data:")) line = line.substring(5).trim();
                    if ("[DONE]".equals(line)) break;
                    JsonObject json;
                    try {
                        json = JsonParser.parseString(line).getAsJsonObject();
                    } catch (Exception ignore) {
                        continue;
                    }
                    if (json.has("successful") && !json.get("successful").getAsBoolean()) {
                        String errCode = json.has("code") ? json.get("code").getAsString() : "?";
                        throw new IllegalStateException(TranslationErrorMessages.youdao(errCode));
                    }
                    if (json.has("code") && !"0".equals(json.get("code").getAsString())) {
                        throw new IllegalStateException(
                                TranslationErrorMessages.youdao(json.get("code").getAsString()));
                    }
                    JsonObject data = json.has("data") && json.get("data").isJsonObject()
                            ? json.getAsJsonObject("data") : null;
                    if (data != null && data.has("transFull")) {
                        lastFull = data.get("transFull").getAsString();
                    }
                }
            }
            if (lastFull == null || lastFull.isEmpty()) {
                throw new IllegalStateException("有道大模型翻译返回结果为空");
            }
            return TranslationResult.success(lastFull);
        } catch (Exception e) {
            LOG.error("有道大模型翻译请求失败", e);
            return TranslationResult.failure(e.getMessage() == null
                    ? "有道大模型翻译请求失败" : e.getMessage());
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