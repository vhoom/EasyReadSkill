package com.translator.api;

public final class TranslateResponse {

    private final String text;
    private final String from;
    private final String to;
    private final String langType;
    private final String requestId;

    /** 构造 TranslateResponse。 */
    private TranslateResponse(Builder b) {
        this.text = b.text;
        this.from = b.from;
        this.to = b.to;
        this.langType = b.langType;
        this.requestId = b.requestId;
    }

    /** 获取Text。 */
    public String getText() { return text; }
    /** 获取From。 */
    public String getFrom() { return from; }
    /** 获取To。 */
    public String getTo() { return to; }
    /** 获取LangType。 */
    public String getLangType() { return langType; }
    /** 获取RequestId。 */
    public String getRequestId() { return requestId; }

    /** 创建 Builder。 */
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String text;
        private String from;
        private String to;
        private String langType;
        private String requestId;

        /** text。 */
        public Builder text(String v) { this.text = v; return this; }
        /** from。 */
        public Builder from(String v) { this.from = v; return this; }
        /** to。 */
        public Builder to(String v) { this.to = v; return this; }
        /** langType。 */
        public Builder langType(String v) { this.langType = v; return this; }
        /** requestId。 */
        public Builder requestId(String v) { this.requestId = v; return this; }

        /** 构建实例。 */
        public TranslateResponse build() { return new TranslateResponse(this); }
    }
}