package com.llm.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.llm.api.LlmConfig;
import com.llm.api.LlmEndpoints;
import com.llm.api.LlmHttpHooks;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OpenAI 兼容 HTTP 客户端：{@code /models} 与 {@code /chat/completions}。
 * 仅依赖 {@link HttpClient} + Gson，不引入 Spring AI。
 *
 * <p>请求是异步发起的：等待响应期间轮询 {@link LlmHttpHooks}，取消时
 * {@code future.cancel(true)} 会真正断开在途请求（同步 {@code send} 做不到这一点）。</p>
 */
public final class OpenAiCompatClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** 等待响应时检查取消的间隔。 */
    private static final long POLL_MILLIS = 200L;

    private static final int ERROR_BODY_LIMIT = 300;

    private final LlmConfig config;
    private final String modelsUrl;
    private final String chatUrl;

    /**
     * @param config 连接配置（含厂商路径信息）
     */
    public OpenAiCompatClient(LlmConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config 不能为空");
        }
        this.config = config;
        this.modelsUrl = LlmEndpoints.modelsUrl(config.vendor(), config.baseUrl());
        this.chatUrl = LlmEndpoints.chatUrl(config.vendor(), config.baseUrl());
    }

    /** @return 当前配置 */
    public LlmConfig config() {
        return config;
    }

    /** @return {@code /models} 完整地址（界面可展示） */
    public String modelsUrl() {
        return modelsUrl;
    }

    /** @return {@code /chat/completions} 完整地址（界面可展示） */
    public String chatUrl() {
        return chatUrl;
    }

    /**
     * GET /models，解析 {@code data[].id}。
     *
     * @return 模型 id 列表
     */
    public List<String> listModels() {
        String body = send(HttpRequest.newBuilder(URI.create(modelsUrl))
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

        String body = send(HttpRequest.newBuilder(URI.create(chatUrl))
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
            throw new IllegalStateException("LLM 返回无 choices: " + truncate(body, ERROR_BODY_LIMIT));
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

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    /**
     * 发请求并等待响应，期间可被 {@link LlmHttpHooks} 取消。
     *
     * @param req 请求
     * @return 响应体
     */
    private static String send(HttpRequest req) {
        CompletableFuture<HttpResponse<String>> future = null;
        try {
            if (LlmHttpHooks.isCancelled()) {
                throw interrupted(null);
            }
            future = HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString());
            while (true) {
                try {
                    return readBody(future.get(POLL_MILLIS, TimeUnit.MILLISECONDS));
                } catch (TimeoutException timeout) {
                    if (LlmHttpHooks.isCancelled()) {
                        throw interrupted(null);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw interrupted(e);
        } catch (CancellationException e) {
            throw interrupted(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IOException io) {
                throw new IllegalStateException("LLM 请求失败: " + io.getMessage(), io);
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("LLM 请求失败: " + cause, cause);
        } finally {
            if (future != null && !future.isDone()) {
                // 真正取消在途交换：释放连接，不再等 120 秒超时
                future.cancel(true);
            }
        }
    }

    private static IllegalStateException interrupted(Throwable cause) {
        InterruptedIOException signal = new InterruptedIOException("已中断");
        if (cause != null) {
            signal.initCause(cause);
        }
        return new IllegalStateException("LLM 请求已中断", signal);
    }

    private static String readBody(HttpResponse<String> resp) {
        int code = resp.statusCode();
        String body = resp.body() == null ? "" : resp.body();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(describeError(code, body));
        }
        return body;
    }

    /**
     * 把错误响应压成一句话：优先 {@code error.message}，其次 {@code message}，最后截断原文。
     *
     * @param code HTTP 状态码
     * @param body 响应体
     * @return 可读的错误描述
     */
    static String describeError(int code, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (!trimmed.isEmpty()) {
            try {
                JsonElement parsed = JsonParser.parseString(trimmed);
                if (parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    JsonElement err = root.get("error");
                    if (err != null && err.isJsonObject()) {
                        JsonObject o = err.getAsJsonObject();
                        if (o.has("message") && !o.get("message").isJsonNull()) {
                            return "HTTP " + code + "（" + o.get("message").getAsString() + "）";
                        }
                        return "HTTP " + code + "（" + truncate(o.toString(), ERROR_BODY_LIMIT) + "）";
                    }
                    if (err != null && err.isJsonPrimitive()) {
                        return "HTTP " + code + "（" + err.getAsString() + "）";
                    }
                    if (root.has("message") && !root.get("message").isJsonNull()) {
                        return "HTTP " + code + "（" + root.get("message").getAsString() + "）";
                    }
                }
            } catch (RuntimeException ignored) {
                // 不是 JSON，按原文截断
            }
            return "HTTP " + code + "（" + truncate(trimmed, ERROR_BODY_LIMIT) + "）";
        }
        return "HTTP " + code;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
