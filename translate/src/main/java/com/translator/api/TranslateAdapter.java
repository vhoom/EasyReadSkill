package com.translator.api;

/**
 * 翻译适配器：无状态、线程安全，同步返回完整结果。
 */
public interface TranslateAdapter {

    /** @return 厂商标识，如 baidu / youdao */
    String vendor();

    /** @return API 标识，如 nmt / llm / domain */
    String api();

    /**
     * 是否支持该请求（默认支持）。
     *
     * @param request 请求
     * @return true 表示可处理
     */
    default boolean supports(TranslateRequest request) {
        return true;
    }

    /**
     * 同步翻译，绝不流式。
     *
     * @param request 请求
     * @return 响应
     */
    TranslateResponse translate(TranslateRequest request);
}
