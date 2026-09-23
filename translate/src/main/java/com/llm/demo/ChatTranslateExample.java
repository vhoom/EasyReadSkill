package com.llm.demo;

import com.llm.api.LlmVendor;
import com.llm.facade.LlmFacade;

import java.util.List;

/**
 * 门面示例：环境变量 → 模型列表 → 翻译。
 *
 * <pre>
 * set DEEPSEEK_API_KEY=sk-...
 * mvn -pl translator exec:java "-Dexec.mainClass=com.llm.demo.ChatTranslateExample" "-Dexec.args=Hello, world|en|zh"
 * </pre>
 *
 * 可选 {@code -Dllm.vendor=DEEPSEEK|ZHIPU|GEMINI|KIMI|OPENAI|QIANWEN}（默认 DEEPSEEK）。
 */
public final class ChatTranslateExample {

    /** 工具类禁止实例化。 */
    private ChatTranslateExample() {}

    /**
     * 入口。
     *
     * @param args 程序参数，推荐 {@code text|from|to}
     */
    public static void main(String[] args) {
        String[] parts = parseArgs(args);
        String text = parts[0];
        String from = parts[1];
        String to = parts[2];

        LlmVendor vendor = resolveVendor();
        LlmFacade llm = LlmFacade.connect(vendor);

        System.out.println("vendor  = " + vendor.displayName());
        System.out.println("baseUrl = " + llm.config().baseUrl());
        System.out.println("model   = " + llm.config().model());
        System.out.println("models  = " + summarize(llm.models()));
        System.out.println("from/to = " + from + " -> " + to);
        System.out.println("----");

        System.out.println(llm.translate(text, from, to));
    }

    /**
     * 解析 {@code -Dllm.vendor}。
     *
     * @return 厂商枚举
     */
    private static LlmVendor resolveVendor() {
        String name = System.getProperty("llm.vendor", "DEEPSEEK");
        try {
            return LlmVendor.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "未知 llm.vendor=" + name + "，可选: DEEPSEEK ZHIPU GEMINI KIMI OPENAI QIANWEN");
        }
    }

    /**
     * 缩短模型列表打印。
     *
     * @param models 模型 id
     * @return 摘要字符串
     */
    private static String summarize(List<String> models) {
        if (models.isEmpty()) {
            return "(empty)";
        }
        if (models.size() <= 8) {
            return models.toString();
        }
        return models.subList(0, 8) + " … +" + (models.size() - 8);
    }

    /**
     * 支持 {@code text|from|to}；IDEA 按空格拆开时先拼回再按 | 分割。
     *
     * @param args 原始参数
     * @return [text, from, to]
     */
    static String[] parseArgs(String[] args) {
        if (args == null || args.length == 0) {
            return new String[] {"Hello, world", "en", "zh"};
        }
        String joined = String.join(" ", args).trim();
        String[] pipe = joined.split("\\|", 3);
        if (pipe.length == 3) {
            return new String[] {pipe[0].trim(), pipe[1].trim(), pipe[2].trim()};
        }
        if (pipe.length == 2) {
            return new String[] {pipe[0].trim(), pipe[1].trim(), "zh"};
        }
        return new String[] {joined, "en", "zh"};
    }
}
