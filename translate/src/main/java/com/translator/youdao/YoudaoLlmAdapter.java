package com.translator.youdao;

import com.translator.api.TranslateAdapter;
import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.youdao.internal.YoudaoLlmResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class YoudaoLlmAdapter implements TranslateAdapter {

    private final YoudaoConfig config;
    private final YoudaoSigner signer;
    private final YoudaoHttpClient http;

    /** 构造 YoudaoLlmAdapter。 */
    public YoudaoLlmAdapter(YoudaoConfig config) {
        this.config = config;
        this.signer = new YoudaoSigner(config);
        this.http = new YoudaoHttpClient();
    }

    /** 返回vendor标识。 */
    public String vendor() { return "youdao"; }
    /** 返回api标识。 */
    public String api() { return "llm"; }

    /** 执行翻译。 */
    public TranslateResponse translate(TranslateRequest req) {
        String salt = UUID.randomUUID().toString();
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String sign = signer.sign(req.getText(), salt, curtime);

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("appKey", config.appKey());
        params.put("salt", salt);
        params.put("curtime", curtime);
        params.put("sign", sign);
        params.put("signType", "v3");
        params.put("i", req.getText());
        params.put("from", req.getFrom());
        params.put("to", req.getTo());
        // 桌面程序禁用流：固定 full
        params.put("streamType", "full");
        String handleOption = req.getExtras().get("handleOption");
        if (handleOption == null || handleOption.length() == 0) {
            handleOption = req.getModel();
        }
        if (handleOption != null && handleOption.length() > 0) {
            params.put("handleOption", handleOption);
        }
        if (req.getPrompt() != null && req.getPrompt().length() > 0) {
            params.put("prompt", req.getPrompt());
        }

        String body = http.postForm(
                "https://openapi.youdao.com/proxy/http/llm-trans", params);
        return YoudaoLlmResponse.parse(body);
    }
}