package com.translator.youdao;

import com.translator.api.TranslateAdapter;
import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.youdao.internal.YoudaoNmtResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class YoudaoNmtAdapter implements TranslateAdapter {

    private final YoudaoConfig config;
    private final YoudaoSigner signer;
    private final YoudaoHttpClient http;

    /** 构造 YoudaoNmtAdapter。 */
    public YoudaoNmtAdapter(YoudaoConfig config) {
        this.config = config;
        this.signer = new YoudaoSigner(config);
        this.http = new YoudaoHttpClient();
    }

    /** 返回vendor标识。 */
    public String vendor() { return "youdao"; }
    /** 返回api标识。 */
    public String api() { return "nmt"; }

    /** 执行翻译。 */
    public TranslateResponse translate(TranslateRequest req) {
        // 方法内局部变量，不共享，线程安全
        String salt = UUID.randomUUID().toString();
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String sign = signer.sign(req.getText(), salt, curtime);

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", req.getText());
        params.put("from", req.getFrom());
        params.put("to", req.getTo());
        params.put("appKey", config.appKey());
        params.put("salt", salt);
        params.put("sign", sign);
        params.put("signType", "v3");
        params.put("curtime", curtime);
        if (req.getDomain() != null && req.getDomain().length() > 0
                && !"general".equals(req.getDomain())) {
            params.put("domain", req.getDomain());
        }

        String body = http.postForm("https://openapi.youdao.com/api", params);
        return YoudaoNmtResponse.parse(body);
    }
}