package com.song.service;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进行中的翻译 HTTP。中断时 disconnect，读会立刻失败，不再把文件写回去。
 */
public final class HttpCalls {

    private static final Set<HttpURLConnection> LIVE = ConcurrentHashMap.newKeySet();
    private static volatile boolean cancelled;

    private HttpCalls() {}

    /** 新一批翻译开始前调用，清掉上一批的中断标记。 */
    public static void arm() {
        cancelled = false;
    }

    public static void cancelAll() {
        cancelled = true;
        for (HttpURLConnection conn : LIVE) {
            conn.disconnect();
        }
    }

    public static boolean isCancelled() {
        return cancelled || Thread.currentThread().isInterrupted();
    }

    public static boolean causedByCancel(Throwable error) {
        if (cancelled || Thread.currentThread().isInterrupted()) return true;
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof InterruptedException || t instanceof InterruptedIOException) return true;
        }
        return false;
    }

    public static HttpURLConnection open(String url) throws InterruptedIOException, java.io.IOException {
        if (isCancelled()) throw new InterruptedIOException("已中断");
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        track(conn);
        if (cancelled) {
            finish(conn);
            throw new InterruptedIOException("已中断");
        }
        return conn;
    }

    /** 登记已由外部打开的连接（如 translator 包），以便 cancelAll 能 disconnect。 */
    public static void track(HttpURLConnection conn) throws InterruptedIOException {
        if (conn == null) return;
        if (isCancelled()) {
            conn.disconnect();
            throw new InterruptedIOException("已中断");
        }
        LIVE.add(conn);
        if (cancelled) {
            finish(conn);
            throw new InterruptedIOException("已中断");
        }
    }

    public static void finish(HttpURLConnection conn) {
        if (conn == null) return;
        LIVE.remove(conn);
        conn.disconnect();
    }
}
