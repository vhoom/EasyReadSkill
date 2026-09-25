package com.song.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 翻译 API 错误码对应的文档说明。
 */
public final class TranslationErrorMessages {

    private static final Map<String, String> BAIDU = Map.ofEntries(
            Map.entry("52000", "成功"),
            Map.entry("52001", "请求超时"),
            Map.entry("52002", "系统错误，请重试"),
            Map.entry("52003", "未授权用户，请检查 appid 是否正确或服务是否已开通"),
            Map.entry("54000", "必填参数为空"),
            Map.entry("54001", "签名错误，请检查签名生成方法"),
            Map.entry("54003", "访问频率受限，请降低调用频率或完成身份认证"),
            Map.entry("54004", "账户余额不足，请前往控制台充值"),
            Map.entry("54005", "长 query 请求频繁，请 3 秒后再试"),
            Map.entry("58000", "客户端 IP 非法，请检查 IP 配置"),
            Map.entry("58001", "译文语言方向不支持"),
            Map.entry("58002", "服务当前已关闭，请在控制台开启服务"),
            Map.entry("58003", "已被封禁，请联系百度翻译客服"),
            Map.entry("90107", "认证未通过或未生效"),
            Map.entry("20000", "请求内容存在安全风险，请检查文本是否包含敏感内容"),
            Map.entry("20001", "请求内容存在安全风险，请检查文本是否包含敏感内容")
    );

    private static final Map<String, String> YOUDAO = Map.ofEntries(
            Map.entry("101", "缺少必填参数"),
            Map.entry("102", "不支持的语言类型"),
            Map.entry("103", "翻译文本过长"),
            Map.entry("108", "应用ID无效"),
            Map.entry("110", "无相关服务的有效应用，请检查应用是否绑定服务"),
            Map.entry("111", "开发者账号无效"),
            Map.entry("112", "请求服务无效"),
            Map.entry("113", "q不能为空"),
            Map.entry("116", "strict 字段取值无效"),
            Map.entry("202", "签名检验失败，请检查应用ID和应用密钥，并确保 q 为 UTF-8 编码"),
            Map.entry("203", "访问 IP 地址不在可访问 IP 列表"),
            Map.entry("206", "时间戳无效导致签名校验失败"),
            Map.entry("207", "重放请求，salt 建议使用 UUID"),
            Map.entry("301", "辞典查询失败"),
            Map.entry("302", "翻译查询失败"),
            Map.entry("303", "服务端其它异常"),
            Map.entry("304", "翻译失败，请联系技术支持"),
            Map.entry("309", "domain 参数错误"),
            Map.entry("310", "未开通领域翻译服务"),
            Map.entry("401", "账户已经欠费，请充值"),
            Map.entry("411", "访问频率受限，请稍后访问"),
            Map.entry("412", "长请求过于频繁，请稍后访问"),
            Map.entry("902000", "大模型翻译调用失败")
    );

    private TranslationErrorMessages() {}

    public static String baidu(String code) {
        return BAIDU.getOrDefault(code, "百度翻译错误，错误码：" + code);
    }

    public static String youdao(String code) {
        return YOUDAO.getOrDefault(code, "有道翻译错误，错误码：" + code);
    }

    private static final Pattern BAIDU_CODE = Pattern.compile("百度错误\\s*(\\d+)");
    private static final Pattern YOUDAO_CODE =
            Pattern.compile("有道(?:大模型)?错误(?:码)?[:：]\\s*(\\d+)");

    /**
     * 给厂商原始错误串补上文档里的中文说明。
     *
     * @param raw 原始错误信息，可为 null
     * @return 带说明的信息；识别不出错误码时原样返回
     */
    public static String explain(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        Matcher baidu = BAIDU_CODE.matcher(raw);
        if (baidu.find()) {
            return raw + " —— " + baidu(baidu.group(1));
        }
        Matcher youdao = YOUDAO_CODE.matcher(raw);
        if (youdao.find()) {
            return raw + " —— " + youdao(youdao.group(1));
        }
        return raw;
    }
}
