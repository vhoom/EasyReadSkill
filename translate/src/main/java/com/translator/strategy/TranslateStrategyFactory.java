package com.translator.strategy;

import com.translator.api.TranslateAdapter;
import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 策略工厂：构造后 Map 只读，线程安全。
 * 上层可并发调用 execute。
 */
public class TranslateStrategyFactory {

    private final Map<String, TranslateAdapter> adapters;

    /** 构造 TranslateStrategyFactory。 */
    public TranslateStrategyFactory(List<TranslateAdapter> adapterList) {
        Map<String, TranslateAdapter> tmp = new HashMap<String, TranslateAdapter>();
        Iterator<TranslateAdapter> it = adapterList.iterator();
        while (it.hasNext()) {
            TranslateAdapter a = it.next();
            tmp.put(key(a.vendor(), a.api()), a);
        }
        // 用不可变视图封装，之后只读，天然线程安全
        this.adapters = java.util.Collections.unmodifiableMap(tmp);
    }

    /** execute。 */
    public TranslateResponse execute(TranslateRequest req) {
        String k = key(req.getVendor(), req.getApi());
        TranslateAdapter adapter = adapters.get(k);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的翻译组合: " + k);
        }
        if (!adapter.supports(req)) {
            throw new IllegalArgumentException("适配器不支持该请求: " + k);
        }
        return adapter.translate(req);
    }

    /** key。 */
    private static String key(String vendor, String api) {
        return vendor + ":" + api;
    }
}