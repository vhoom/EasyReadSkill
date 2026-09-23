package com.translator.youdao;

public final class YoudaoConfig {

    private final String appKey;
    private final String appSecret;

    public YoudaoConfig(String appKey, String appSecret) {
        if (appKey == null || appKey.length() == 0) {
            throw new IllegalArgumentException("appKey 不能为空");
        }
        if (appSecret == null || appSecret.length() == 0) {
            throw new IllegalArgumentException("appSecret 不能为空");
        }
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    /** appKey。 */
    public String appKey() { return appKey; }
    /** appSecret。 */
    public String appSecret() { return appSecret; }
}