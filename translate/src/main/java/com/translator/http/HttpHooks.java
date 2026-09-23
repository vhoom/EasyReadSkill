package com.translator.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;

/**
 * 可选 HTTP 生命周期钩子，供宿主接入取消/追踪（如主应用 HttpCalls）。
 * 默认仅检查线程中断。
 */
public final class HttpHooks {

    public interface Listener {
        /** 连接已打开、即将发请求；可登记到宿主的可取消集合 */
        void onOpen(HttpURLConnection conn) throws IOException;

        /** 请求结束（成功或失败），应从可取消集合移除 */
        void onClose(HttpURLConnection conn);

        /** 是否应中止当前请求 */
        boolean isCancelled();
    }

    private static final Listener DEFAULT = new Listener() {
        @Override
        /** onOpen。 */
        public void onOpen(HttpURLConnection conn) {}

        @Override
        /** onClose。 */
        public void onClose(HttpURLConnection conn) {}

        @Override
        /** 是否Cancelled。 */
        public boolean isCancelled() {
            return Thread.currentThread().isInterrupted();
        }
    };

    private static volatile Listener listener = DEFAULT;

    /** 构造 HttpHooks。 */
    private HttpHooks() {}

    /** 设置Listener。 */
    public static void setListener(Listener next) {
        listener = next != null ? next : DEFAULT;
    }

    /** 获取Listener。 */
    public static Listener getListener() {
        return listener;
    }

    /** checkCancelled。 */
    public static void checkCancelled() throws InterruptedIOException {
        if (listener.isCancelled()) {
            throw new InterruptedIOException("已中断");
        }
    }
}
