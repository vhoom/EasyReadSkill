package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.HttpCalls;
import com.song.service.TranslationResult;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 百度大模型文本翻译 API。
 */
public class BaiduLlmTranslationProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BaiduLlmTranslationProvider.class);
    private static final String API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    @Override
    public ProviderType getType() { return ProviderType.BAIDU_LLM; }

    @Override
    public TranslationResult translate(String q, String from, String to, AppConfig config) {
        try {
            String salt = String.valueOf(System.currentTimeMillis());
            String sign = BaiduApiUtils.md5(
                    config.getAppId() + BaiduApiUtils.signText(q) + salt + config.getSecretKey());
            JsonObject body = new JsonObject();
            body.addProperty("q", q);
            body.addProperty("from", from);
            body.addProperty("to", to);
            body.addProperty("appid", config.getAppId());
            body.addProperty("salt", salt);
            body.addProperty("sign", sign);
            body.addProperty("model_type", 2);

            JsonObject json = BaiduApiUtils.postJson(
                    API_URL, body.toString(), "Bearer " + config.getApiKey());
            return TranslationResult.success(BaiduApiUtils.extractDst(json));
        } catch (Exception e) {
            if (HttpCalls.causedByCancel(e)) return TranslationResult.interrupted();
            LOG.error("百度大模型翻译请求失败", e);
            return TranslationResult.failure(e.getMessage() == null
                    ? "百度大模型翻译请求失败" : e.getMessage());
        }
    }
}