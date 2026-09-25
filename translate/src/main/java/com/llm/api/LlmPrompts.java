package com.llm.api;

/**
 * LLM 默认提示词（与翻译主包无关）。
 */
public final class LlmPrompts {

    private LlmPrompts() {}

    /** 批量翻译的段标记前缀，例如 {@code <<<EASEREAD#1>>>}。 */
    public static final String BATCH_MARKER_PREFIX = "<<<EASEREAD#";

    /** 批量翻译的段标记后缀。 */
    public static final String BATCH_MARKER_SUFFIX = ">>>";

    /**
     * @param index 段号（从 1 开始）
     * @return 段标记文本
     */
    public static String batchMarker(int index) {
        return BATCH_MARKER_PREFIX + index + BATCH_MARKER_SUFFIX;
    }

    /** 默认系统提示词 */
    public static final String DEFAULT_TRANSLATE_SYSTEM =
            "你是名翻译全球语言翻译专家，精通各种国家文化习俗思维地道方言俚语和表达习惯，"
                    + "并且能准确的翻译成其它国家的语言且符合当地人们的表达思维习惯，"
                    + "可适当调整语序逻辑但仍要保持与含义原文一致。"
                    + "对于特殊的句子字符串不应该强行翻译如代码、变量、命令、突然创造命名出的一个直译没意义的名词。"
                    + "只输出译文，不要解释。";

    /**
     * 批量翻译的系统提示：在原提示词后面加上"逐段翻译、原样保留标记、不要合并"的硬约束。
     *
     * @param userPrompt 用户自定义提示词；空则用默认
     * @return 系统提示
     */
    public static String batchSystem(String userPrompt) {
        String base = (userPrompt == null || userPrompt.isBlank())
                ? DEFAULT_TRANSLATE_SYSTEM : userPrompt.trim();
        return base
                + "\n\n本次会一次给出多段文本，每段前面有一行标记，形如 "
                + batchMarker(1) + "。必须遵守："
                + "1) 每段独立翻译，各段之间不得互相借用、合并或省略内容；"
                + "2) 标记必须原样保留在对应译文前面，不要翻译、改写、增删标记；"
                + "3) 输出顺序与输入一致；"
                + "4) 只输出标记与译文，不要任何解释或额外文字。";
    }
}
