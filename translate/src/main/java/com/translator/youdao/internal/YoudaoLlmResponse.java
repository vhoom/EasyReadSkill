package com.translator.youdao.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.translator.api.TranslateResponse;

/**
 * 有道大模型响应解析，包内可见。
 * 非流式使用：把全部 SSE 行一次性聚合为完整结果。
 */
public final class YoudaoLlmResponse {

    /** 构造 YoudaoLlmResponse。 */
    private YoudaoLlmResponse() {}

    /** parse。 */
    public static TranslateResponse parse(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalStateException("有道大模型返回空响应");
        }

        // 非 SSE：整段 JSON 错误体
        String trimmed = body.trim();
        if (!trimmed.contains("data:") && trimmed.startsWith("{")) {
            tryParseErrorObject(trimmed);
        }

        StringBuilder incre = new StringBuilder();
        String full = "";
        String langType = "";
        String requestId = "";
        boolean sawData = false;

        String[] lines = body.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("data:")) {
                line = line.substring(5).trim();
            }
            if (line.length() == 0 || !line.startsWith("{")) {
                continue;
            }
            try {
                JsonObject node = JsonParser.parseString(line).getAsJsonObject();
                checkCode(node);

                if (node.has("data")) {
                    sawData = true;
                    JsonObject data = node.getAsJsonObject("data");
                    if (data.has("transIncre")) {
                        incre.append(data.get("transIncre").getAsString());
                    }
                    if (data.has("transFull")) {
                        full = data.get("transFull").getAsString();
                    }
                    if (data.has("langType")) {
                        langType = data.get("langType").getAsString();
                    }
                }

                if (node.has("requestId")) {
                    requestId = node.get("requestId").getAsString();
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception ignored) {
                // 忽略无法解析的单行（如 event: / id:）
            }
        }

        String text = (full.length() > 0) ? full : incre.toString();
        if (!sawData && text.length() == 0) {
            throw new IllegalStateException("有道大模型无有效译文: " + truncate(body, 200));
        }

        return TranslateResponse.builder()
                .text(text)
                .langType(langType)
                .requestId(requestId)
                .build();
    }

    /** tryParseErrorObject。 */
    private static void tryParseErrorObject(String json) {
        try {
            JsonObject node = JsonParser.parseString(json).getAsJsonObject();
            if (node.has("errorCode")) {
                String code = node.get("errorCode").getAsString();
                if (!"0".equals(code)) {
                    throw new IllegalStateException("有道大模型错误码: " + code);
                }
            }
            checkCode(node);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ignored) {
            // 交给后续 SSE 解析
        }
    }

    /** checkCode。 */
    private static void checkCode(JsonObject node) {
        String code = node.has("code")
                ? node.get("code").getAsString()
                : "0";
        if (!"0".equals(code)) {
            String msg = node.has("message")
                    ? node.get("message").getAsString()
                    : "";
            throw new IllegalStateException("有道大模型错误: " + code
                    + (msg.length() == 0 ? "" : " " + msg));
        }
    }

    /** truncate。 */
    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
