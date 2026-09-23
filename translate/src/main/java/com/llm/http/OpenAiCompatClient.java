package com.llm.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.llm.api.LlmConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容 HTTP 客户端：{@code /models} 与 {@code /chat/completions}。
 * 仅依赖 {@link java.net.http.HttpClient} + Gson，不引入 Spring AI。
 */
public final class OpenAiCompatClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final LlmConfig config;
    private final String clientBase;
    private final String modelsUri;
    private final String completionsUri;

    /**
     * @param config 连接配置（含厂商路径信息）
     */
    public OpenAiCompatClient(LlmConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config 不能为空");
        }
        this.config = config;
        this.clientBase = clientBaseUrl(config);
        this.modelsUri = relativeUri(config.vendor().modelsPath(), clientBase);
        this.completionsUri = relativeUri(config.vendor().completionsPath(), clientBase);
    }

    /** @return 当前配置 */
    public LlmConfig config() {
        return config;
    }

    /**
     * GET /models，解析 {@code data[].id}。
     *
     * @return 模型 id 列表
     */
    public List<String> listModels() {
        String body = send(HttpRequest.newBuilder(URI.create(clientBase + modelsUri))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Accept", "application/json")
                .GET()
                .build());
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray data = root.has("data") && root.get("data").isJsonArray()
                ? root.getAsJsonArray("data") : new JsonArray();
        List<String> ids = new ArrayList<>(data.size());
        for (JsonElement el : data) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject m = el.getAsJsonObject();
            if (m.has("id") && !m.get("id").isJsonNull()) {
                ids.add(m.get("id").getAsString());
            }
        }
        return ids;
    }

    /**
     * POST /chat/completions（非流式）。
     *
     * @param model  模型；空则用配置默认
     * @param system 系统提示，可空
     * @param user   用户消息
     * @return assistant 文本
     */
    public String chat(String model, String system, String user) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user 不能为空");
        }
        String m = (model == null || model.isBlank()) ? config.model() : model.trim();

        JsonArray messages = new JsonArray();
        if (system != null && !system.isBlank()) {
            messages.add(msg("system", system));
        }
        messages.add(msg("user", user));

        JsonObject payload = new JsonObject();
        payload.addProperty("model", m);
        payload.addProperty("temperature", 0.2);
        payload.add("messages", messages);

        String body = send(HttpRequest.newBuilder(URI.create(clientBase + completionsUri))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build());

        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (root.has("error") && root.get("error").isJsonObject()) {
            JsonObject err = root.getAsJsonObject("error");
            String message = err.has("message") ? err.get("message").getAsString() : err.toString();
            throw new IllegalStateException("LLM 错误: " + message);
        }
        JsonArray choices = root.has("choices") && root.get("choices").isJsonArray()
                ? root.getAsJsonArray("choices") : null;
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("LLM 返回无 choices: " + body);
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                ? choice.getAsJsonObject("message") : null;
        if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
            throw new IllegalStateException("LLM 返回空内容");
        }
        String out = message.get("content").getAsString();
        if (out == null || out.isBlank()) {
            throw new IllegalStateException("LLM 返回空内容");
        }
        return out.trim();
    }

    /**
     * OpenAI 兼容 baseUrl（可直接拼 /models、/chat/completions）。
     */
    static String clientBaseUrl(LlmConfig config) {
        String base = config.baseUrl();
        String path = config.vendor().completionsPath();
        if (path.startsWith("/v1/") && !base.endsWith("/v1")) {
            return base + "/v1";
        }
        return base;
    }

    private static String relativeUri(String path, String clientBase) {
        if (clientBase.endsWith("/v1") && path.startsWith("/v1/")) {
            return path.substring(3);
        }
        return path;
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    private static String send(HttpRequest req) {
        try {
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            String body = resp.body() == null ? "" : resp.body();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + ": " + body);
            }
            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 请求已中断", e);
        } catch (IOException e) {
            throw new IllegalStateException("LLM 请求失败: " + e.getMessage(), e);
        }
    }
}
