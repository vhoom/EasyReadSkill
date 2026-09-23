package com.translator.baidu;

public final class BaiduConfig {

    private final String appId;
    private final String secret;

    /** 构造 BaiduConfig。 */
    public BaiduConfig(String appId, String secret) {
        if (appId == null || appId.length() == 0) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        if (secret == null || secret.length() == 0) {
            throw new IllegalArgumentException("secret 不能为空");
        }
        this.appId = appId;
        this.secret = secret;
    }

    /** appId。 */
    public String appId() { return appId; }
    /** secret。 */
    public String secret() { return secret; }
}