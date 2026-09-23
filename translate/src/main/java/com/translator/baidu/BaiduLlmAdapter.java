package com.translator.baidu;

import com.translator.api.TranslateAdapter;
import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.baidu.internal.BaiduResponse;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaiduLlmAdapter implements TranslateAdapter {

    private final BaiduConfig config;
    private final BaiduSigner signer;
    private final BaiduHttpClient http;
    private final SecureRandom random = new SecureRandom();

    /** 构造 BaiduLlmAdapter。 */
    public BaiduLlmAdapter(BaiduConfig config) {
        this.config = config;
        this.signer = new BaiduSigner(config);
        this.http = new BaiduHttpClient();
    }

    /** 返回vendor标识。 */
    public String vendor() { return "baidu"; }
    /** 返回api标识。 */
    public String api() { return "llm"; }

    /** 执行翻译。 */
    public TranslateResponse translate(TranslateRequest req) {
        String salt = String.valueOf(random.nextInt(90000) + 10000);
        String sign = signer.sign(req.getText(), salt);

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("appid", config.appId());
        params.put("q", req.getText());
        params.put("from", req.getFrom());
        params.put("to", req.getTo());
        params.put("salt", salt);
        params.put("sign", sign);
        if (req.getModel() != null) {
            params.put("model_type", req.getModel());
        }
        if (req.getPrompt() != null) {
            params.put("reference", req.getPrompt());
        }

        String body = http.postForm(
                "https://fanyi-api.baidu.com/ait/api/aiTextTranslate", params);
        return BaiduResponse.parse(body);
    }
}