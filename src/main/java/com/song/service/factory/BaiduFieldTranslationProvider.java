package com.song.service.factory;

import com.song.config.AppConfig;
import com.song.model.ProviderType;
import com.song.service.HttpCalls;
import com.song.service.TranslationResult;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 百度领域翻译 API。
 */
public class BaiduFieldTranslationProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BaiduFieldTranslationProvider.class);
    private static final String API_URL = "https://fanyi-api.baidu.com/api/trans/vip/fieldtranslate";

    @Override
    public ProviderType getType() { return ProviderType.BAIDU_FIELD; }

    @Override
    public TranslationResult translate(String q, String from, String to, AppConfig config) {
        try {
            String salt = String.valueOf(System.currentTimeMillis());
            String domain = config.getDomain();
            String sign = BaiduApiUtils.md5(
                    config.getAppId() + BaiduApiUtils.signText(q) + salt
                            + domain + config.getSecretKey());
            String form = "q=" + BaiduApiUtils.urlEncode(q)
                    + "&from=" + BaiduApiUtils.urlEncode(from)
                    + "&to=" + BaiduApiUtils.urlEncode(to)
                    + "&appid=" + BaiduApiUtils.urlEncode(config.getAppId())
                    + "&salt=" + BaiduApiUtils.urlEncode(salt)
                    + "&domain=" + BaiduApiUtils.urlEncode(domain)
                    + "&sign=" + sign;
            JsonObject json = BaiduApiUtils.postForm(API_URL, form);
            return TranslationResult.success(BaiduApiUtils.extractDst(json));
        } catch (Exception e) {
            if (HttpCalls.causedByCancel(e)) return TranslationResult.interrupted();
            LOG.error("百度领域翻译请求失败", e);
            return TranslationResult.failure(e.getMessage() == null
                    ? "百度领域翻译请求失败" : e.getMessage());
        }
    }
}