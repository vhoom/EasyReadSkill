package com.translator.bootstrap;

import com.translator.api.TranslateAdapter;
import com.translator.baidu.BaiduConfig;
import com.translator.baidu.BaiduDomainAdapter;
import com.translator.baidu.BaiduGeneralAdapter;
import com.translator.baidu.BaiduLlmAdapter;
import com.translator.config.CredentialStore;
import com.translator.config.StoredCredentials;
import com.translator.facade.TranslateService;
import com.translator.strategy.TranslateStrategyFactory;
import com.translator.youdao.YoudaoConfig;
import com.translator.youdao.YoudaoLlmAdapter;
import com.translator.youdao.YoudaoNmtAdapter;

import java.util.ArrayList;
import java.util.List;

public final class TranslatorBootstrap {

    /** 构造 TranslatorBootstrap。 */
    private TranslatorBootstrap() {}

    /**
     * 从 {@code ~/.easyReadSkill/config.json} 读取密钥并组装服务。
     * 某厂商密钥不全则跳过该厂商。
     */
    public static TranslateService createFromHomeConfig() {
        StoredCredentials creds = CredentialStore.load();
        return create(creds.toYoudaoConfigOrNull(), creds.toBaiduConfigOrNull());
    }

    /**
     * @param youdao 有道配置；为 null 时不注册有道适配器
     * @param baidu  百度配置；为 null 时不注册百度适配器
     */
    public static TranslateService create(YoudaoConfig youdao, BaiduConfig baidu) {
        if (youdao == null && baidu == null) {
            throw new IllegalArgumentException("至少需要有道或百度之一的配置");
        }

        List<TranslateAdapter> adapters = new ArrayList<TranslateAdapter>();
        if (youdao != null) {
            adapters.add(new YoudaoNmtAdapter(youdao));
            adapters.add(new YoudaoLlmAdapter(youdao));
        }
        if (baidu != null) {
            adapters.add(new BaiduGeneralAdapter(baidu));
            adapters.add(new BaiduLlmAdapter(baidu));
            adapters.add(new BaiduDomainAdapter(baidu));
        }

        TranslateStrategyFactory factory = new TranslateStrategyFactory(adapters);
        return new TranslateService(factory);
    }
}
