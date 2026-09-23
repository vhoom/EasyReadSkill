package com.translator.youdao.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.translator.api.TranslateResponse;

/** 有道 NMT 响应解析，包内可见。 */
public final class YoudaoNmtResponse {

    private YoudaoNmtResponse() {}

    public static TranslateResponse parse(String body) {
        JsonObject node = JsonParser.parseString(body).getAsJsonObject();

        String code = node.has("errorCode")
                ? node.get("errorCode").getAsString()
                : "0";
        if (!"0".equals(code)) {
            throw new IllegalStateException("有道错误码: " + code);
        }

        String text = "";
        if (node.has("translation")) {
            JsonArray arr = node.getAsJsonArray("translation");
            if (arr.size() > 0) {
                text = arr.get(0).getAsString();
            }
        }

        String l = node.has("l") ? node.get("l").getAsString() : "";

        return TranslateResponse.builder()
                .text(text)
                .langType(l)
                .build();
    }
}