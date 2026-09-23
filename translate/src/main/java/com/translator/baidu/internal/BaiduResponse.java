package com.translator.baidu.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.translator.api.TranslateResponse;

/** 百度响应解析，包内可见。无状态，线程安全。 */
public final class BaiduResponse {

    /** 构造 BaiduResponse。 */
    private BaiduResponse() {}

    /** parse。 */
    public static TranslateResponse parse(String body) {
        JsonObject node = JsonParser.parseString(body).getAsJsonObject();

        if (node.has("error_code")) {
            String code = node.get("error_code").getAsString();
            if (!"52000".equals(code)) {
                String msg = node.has("error_msg")
                        ? node.get("error_msg").getAsString()
                        : "";
                throw new IllegalStateException("百度错误 " + code + ": " + msg);
            }
        }

        StringBuilder text = new StringBuilder();
        if (node.has("trans_result")) {
            JsonArray arr = node.getAsJsonArray("trans_result");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                if (!item.has("dst")) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(item.get("dst").getAsString());
            }
        }

        String from = node.has("from") ? node.get("from").getAsString() : "";
        String to = node.has("to") ? node.get("to").getAsString() : "";

        return TranslateResponse.builder()
                .text(text.toString())
                .from(from)
                .to(to)
                .build();
    }
}
