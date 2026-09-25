package com.llm.api;

import java.io.InterruptedIOException;

/**
 * {@code com.llm} 的 HTTP 取消钩子。
 *
 * <p>默认只认线程中断；宿主（主应用）可 {@link #setListener} 接上自己的取消标志，
 * 使"中断翻译"能真正掐断在途的大模型请求。</p>
 */
public final class LlmHttpHooks {

    /** 取消判定。 */
    public interface Listener {
        /** @return true 表示应中止当前请求 */
        boolean isCancelled();
    }

    private static final Listener DEFAULT = () -> Thread.currentThread().isInterrupted();

    private static volatile Listener listener = DEFAULT;

    private LlmHttpHooks() {}

    /**
     * 安装监听器。
     *
     * @param next 监听器；null 恢复默认
     */
    public static void setListener(Listener next) {
        listener = next != null ? next : DEFAULT;
    }

    /** @return 当前监听器 */
    public static Listener getListener() {
        return listener;
    }

    /** @return 是否应中止 */
    public static boolean isCancelled() {
        return listener.isCancelled();
    }

    /** @throws InterruptedIOException 应中止时 */
    public static void checkCancelled() throws InterruptedIOException {
        if (isCancelled()) {
            throw new InterruptedIOException("已中断");
        }
    }
}
