package com.song.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 唯一后台线程：所有网络（及扫描等勿堵 UI 的活）都排队到这里。
 * JavaFX 应用线程只负责界面。
 */
public final class NetworkWorker {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "network");
        t.setDaemon(true);
        return t;
    });

    private NetworkWorker() {}

    /**
     * @param task 后台任务
     * @return 可中断的 Future
     */
    public static Future<?> submit(Runnable task) {
        return EXEC.submit(task);
    }
}
