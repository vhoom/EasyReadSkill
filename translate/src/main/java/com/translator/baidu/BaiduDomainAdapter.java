package com.translator.baidu;

import com.translator.api.TranslateAdapter;
import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.baidu.internal.BaiduResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaiduDomainAdapter implements TranslateAdapter {

    private final BaiduConfig config;
    private final BaiduSigner signer;
    private final BaiduHttpClient http;

    /** 构造 BaiduDomainAdapter。 */
    public BaiduDomainAdapter(BaiduConfig config) {
        this.config = config;
        this.signer = new BaiduSigner(config);
        this.http = new BaiduHttpClient();
    }

    /** 返回vendor标识。 */
    public String vendor() { return "baidu"; }
    /** 返回api标识。 */
    public String api() { return "domain"; }

    /** 是否支持该请求。 */
    public boolean supports(TranslateRequest req) {
        return req.getDomain() != null && req.getDomain().length() > 0;
    }

    /** 执行翻译。 */
    public TranslateResponse translate(TranslateRequest req) {
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = signer.signDomain(req.getText(), salt, req.getDomain());

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", req.getText());
        params.put("from", req.getFrom());
        params.put("to", req.getTo());
        params.put("appid", config.appId());
        params.put("salt", salt);
        params.put("domain", req.getDomain());
        params.put("sign", sign);

        String body = http.postForm(
                "https://fanyi-api.baidu.com/api/trans/vip/fieldtranslate", params);
        return BaiduResponse.parse(body);
    }
}