package com.translator.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 不可变请求对象，天然线程安全 */
public final class TranslateRequest {

    private final String vendor;
    private final String api;
    private final String text;
    private final String from;
    private final String to;
    private final String prompt;
    private final String domain;
    private final String model;
    private final Map<String, String> extras;

    /** 构造 TranslateRequest。 */
    private TranslateRequest(Builder b) {
        this.vendor = b.vendor;
        this.api = b.api;
        this.text = b.text;
        this.from = b.from;
        this.to = b.to;
        this.prompt = b.prompt;
        this.domain = b.domain;
        this.model = b.model;
        this.extras = Collections.unmodifiableMap(new HashMap<String, String>(b.extras));
    }

    /** 获取Vendor。 */
    public String getVendor() { return vendor; }
    /** 获取Api。 */
    public String getApi() { return api; }
    /** 获取Text。 */
    public String getText() { return text; }
    /** 获取From。 */
    public String getFrom() { return from; }
    /** 获取To。 */
    public String getTo() { return to; }
    /** 获取Prompt。 */
    public String getPrompt() { return prompt; }
    /** 获取Domain。 */
    public String getDomain() { return domain; }
    /** 获取Model。 */
    public String getModel() { return model; }
    /** 获取Extras。 */
    public Map<String, String> getExtras() { return extras; }

    /** 创建 Builder。 */
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String vendor;
        private String api;
        private String text;
        private String from = "auto";
        private String to;
        private String prompt;
        private String domain;
        private String model;
        private final Map<String, String> extras = new HashMap<String, String>();

        /** 返回vendor标识。 */
        public Builder vendor(String v) { this.vendor = v; return this; }
        /** 返回api标识。 */
        public Builder api(String v) { this.api = v; return this; }
        /** text。 */
        public Builder text(String v) { this.text = v; return this; }
        /** from。 */
        public Builder from(String v) { this.from = v; return this; }
        /** to。 */
        public Builder to(String v) { this.to = v; return this; }
        /** prompt。 */
        public Builder prompt(String v) { this.prompt = v; return this; }
        /** domain。 */
        public Builder domain(String v) { this.domain = v; return this; }
        /** model。 */
        public Builder model(String v) { this.model = v; return this; }
        /** extra。 */
        public Builder extra(String k, String v) { this.extras.put(k, v); return this; }

        /** 构建实例。 */
        public TranslateRequest build() {
            if (text == null || text.length() == 0) {
                throw new IllegalArgumentException("text 不能为空");
            }
            if (vendor == null || api == null) {
                throw new IllegalArgumentException("vendor/api 不能为空");
            }
            if (to == null) {
                throw new IllegalArgumentException("to 不能为空");
            }
            return new TranslateRequest(this);
        }
    }
}