package com.song.service.factory;

import com.llm.api.LlmConfig;
import com.llm.api.LlmVendor;
import com.llm.facade.LlmFacade;
import com.song.config.AppConfig;
import com.song.config.BaiduConfig;
import com.song.config.LlmConfigs;
import com.song.config.LlmSlotConfig;
import com.song.config.YoudaoConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.service.HttpCalls;
import com.song.service.TranslationResult;
import com.translator.api.TranslateRequest;
import com.translator.bootstrap.TranslatorBootstrap;
import com.translator.facade.TranslateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将主应用的 ProviderType 委托给 translate 模块（百度/有道）或 com.llm 门面。
 */
final class TranslatorBackedProvider implements TranslationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TranslatorBackedProvider.class);

    private final ProviderType type;

    TranslatorBackedProvider(ProviderType type) {
        this.type = type != null ? type : ProviderType.getDefault();
    }

    @Override
    public ProviderType getType() {
        return type;
    }

    @Override
    public TranslationResult translate(String text, String from, String to, AppConfig config) {
        if (HttpCalls.isCancelled()) {
            return TranslationResult.interrupted();
        }
        try {
            if (type.getVendor().isLlm()) {
                return TranslationResult.success(llmTranslate(text, from, to, config));
            }
            TranslateService service = buildService(config);
            return TranslationResult.success(invoke(service, text, from, to, config));
        } catch (Exception e) {
            if (HttpCalls.causedByCancel(e)) {
                return TranslationResult.interrupted();
            }
            LOG.error("{} 翻译失败", type.getDisplayName(), e);
            return TranslationResult.failure(e.getMessage() == null
                    ? type.getDisplayName() + "失败" : e.getMessage());
        }
    }

    private String llmTranslate(String text, String from, String to, AppConfig config) {
        ProviderVendor vendor = type.getVendor();
        LlmVendor llmVendor = LlmConfigs.toLlmVendor(vendor);
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        if (slot == null) {
            throw new IllegalStateException("未找到 " + vendor.getDisplayName() + " 配置");
        }
        String apiKey = slot.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            String fromEnv = LlmConfig.firstEnv(llmVendor.apiKeyEnvs());
            if (fromEnv == null) {
                throw new IllegalStateException(
                        vendor.getDisplayName() + " 未配置 API Key，请在 API 配置中填写或设置环境变量 "
                                + String.join(" / ", llmVendor.apiKeyEnvs()));
            }
            apiKey = fromEnv;
        }
        LlmConfig llmConfig = new LlmConfig(
                llmVendor,
                slot.resolveBaseUrl(llmVendor),
                apiKey,
                slot.resolveModel(llmVendor));
        return LlmFacade.of(llmConfig).translate(
                null, text, from, to, slot.getPrompt());
    }

    private String invoke(TranslateService service, String text, String from, String to,
                          AppConfig config) {
        return switch (type) {
            case BAIDU_GENERAL -> service.translate(baiduRequest("nmt", text, from, to, config)).getText();
            case BAIDU_FIELD -> service.translate(baiduRequest("domain", text, from, to, config)).getText();
            case BAIDU_LLM -> service.translate(baiduRequest("llm", text, from, to, config)).getText();
            case YOUDAO_TEXT -> service.translate(youdaoTextRequest(text, from, to, config)).getText();
            case YOUDAO_LLM -> youdaoLlm(service, text, from, to, config);
            default -> throw new IllegalStateException("非百度/有道类型应由 llmTranslate 处理: " + type);
        };
    }

    private static String youdaoLlm(TranslateService service, String text, String from, String to,
                                    AppConfig config) {
        String handleOption = config.getHandleOption();
        try {
            return service.translate(youdaoLlmRequest(text, from, to, config, handleOption)).getText();
        } catch (RuntimeException first) {
            if (HttpCalls.causedByCancel(first)) {
                throw first;
            }
            if (isHttp400(first) && handleOption != null && !"0".equals(handleOption)) {
                LOG.warn("有道大模型 handleOption={} 被拒绝，回退使用 0", handleOption);
                return service.translate(youdaoLlmRequest(text, from, to, config, "0")).getText();
            }
            throw first;
        }
    }

    private static boolean isHttp400(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("HTTP 400")) {
                return true;
            }
        }
        return false;
    }

    private static TranslateRequest baiduRequest(String api, String text, String from, String to,
                                                 AppConfig config) {
        TranslateRequest.Builder b = TranslateRequest.builder()
                .vendor("baidu")
                .api(api)
                .text(text)
                .from(from)
                .to(to);
        if ("domain".equals(api)) {
            b.domain(config.getDomain());
        }
        if ("llm".equals(api)) {
            b.model("llm");
            String prompt = config.getBaiduConfig().getPrompt();
            if (prompt != null && !prompt.isBlank()) {
                b.prompt(prompt);
            }
        }
        return b.build();
    }

    private static TranslateRequest youdaoTextRequest(String text, String from, String to,
                                                      AppConfig config) {
        TranslateRequest.Builder b = TranslateRequest.builder()
                .vendor("youdao")
                .api("nmt")
                .text(text)
                .from(YoudaoLanguageMapper.map(from))
                .to(YoudaoLanguageMapper.map(to));
        String domain = config.getYoudaoDomain();
        if (domain != null && !domain.isBlank() && !"general".equals(domain)) {
            b.domain(domain);
        }
        return b.build();
    }

    private static TranslateRequest youdaoLlmRequest(String text, String from, String to,
                                                     AppConfig config, String handleOption) {
        TranslateRequest.Builder b = TranslateRequest.builder()
                .vendor("youdao")
                .api("llm")
                .text(text)
                .from(YoudaoLanguageMapper.map(from))
                .to(YoudaoLanguageMapper.map(to));
        if (handleOption != null && !handleOption.isBlank()) {
            b.extra("handleOption", handleOption);
        }
        String prompt = config.getYoudaoConfig().getPrompt();
        if (prompt != null && !prompt.isBlank()) {
            b.prompt(prompt);
        }
        return b.build();
    }

    private static TranslateService buildService(AppConfig config) {
        YoudaoConfig y = config.getYoudaoConfig();
        BaiduConfig b = config.getBaiduConfig();

        com.translator.youdao.YoudaoConfig youdao = null;
        com.translator.baidu.BaiduConfig baidu = null;

        if (y.getAppId() != null && !y.getAppId().isBlank()
                && y.getSecretKey() != null && !y.getSecretKey().isBlank()) {
            youdao = new com.translator.youdao.YoudaoConfig(y.getAppId().trim(), y.getSecretKey().trim());
        }
        if (b.getAppId() != null && !b.getAppId().isBlank()) {
            String secret = b.getSecretKey();
            if (secret != null && !secret.isBlank()) {
                baidu = new com.translator.baidu.BaiduConfig(b.getAppId().trim(), secret.trim());
            }
        }

        if (youdao == null && baidu == null) {
            throw new IllegalStateException("请先在 API 配置中填写对应厂商的密钥");
        }
        return TranslatorBootstrap.create(youdao, baidu);
    }
}
