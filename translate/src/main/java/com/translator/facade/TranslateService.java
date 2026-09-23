package com.translator.facade;

import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.strategy.TranslateStrategyFactory;

/**
 * 门面：唯一业务入口。
 * 无状态，线程安全，可被多线程并发调用。
 */
public class TranslateService {

    private final TranslateStrategyFactory factory;

    /** 构造 TranslateService。 */
    public TranslateService(TranslateStrategyFactory factory) {
        this.factory = factory;
    }

    /** 执行翻译。 */
    public TranslateResponse translate(TranslateRequest req) {
        if (req.getText() == null || req.getText().length() == 0) {
            throw new IllegalArgumentException("text 不能为空");
        }
        return factory.execute(req);
    }

    /** 有道文本翻译。 */
    public String youdaoNmt(String text, String from, String to) {
        return translate(TranslateRequest.builder()
                .vendor("youdao").api("nmt")
                .text(text).from(from).to(to)
                .build()).getText();
    }

    /** 有道大模型翻译。 */
    public String youdaoLlm(String text, String from, String to, String prompt) {
        return translate(TranslateRequest.builder()
                .vendor("youdao").api("llm")
                .text(text).from(from).to(to)
                .prompt(prompt)
                .build()).getText();
    }

    /** 百度通用翻译。 */
    public String baiduGeneral(String text, String from, String to) {
        return translate(TranslateRequest.builder()
                .vendor("baidu").api("nmt")
                .text(text).from(from).to(to)
                .build()).getText();
    }

    /** 百度大模型翻译。 */
    public String baiduLlm(String text, String from, String to, String prompt) {
        return translate(TranslateRequest.builder()
                .vendor("baidu").api("llm")
                .text(text).from(from).to(to)
                .prompt(prompt).model("llm")
                .build()).getText();
    }

    /** 百度领域翻译。 */
    public String baiduDomain(String text, String from, String to, String domain) {
        return translate(TranslateRequest.builder()
                .vendor("baidu").api("domain")
                .text(text).from(from).to(to)
                .domain(domain)
                .build()).getText();
    }
}