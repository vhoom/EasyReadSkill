package com.song.service.factory;

import com.song.service.HttpCalls;
import com.translator.http.HttpHooks;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * 把主应用的 HttpCalls 接到翻译包 HttpHooks，使「停止翻译」能断开进行中的请求。
 */
final class TranslatorHttpBridge {

    private static final HttpHooks.Listener LISTENER = new HttpHooks.Listener() {
        @Override
        public void onOpen(HttpURLConnection conn) throws IOException {
            HttpCalls.track(conn);
        }

        @Override
        public void onClose(HttpURLConnection conn) {
            HttpCalls.finish(conn);
        }

        @Override
        public boolean isCancelled() {
            return HttpCalls.isCancelled();
        }
    };

    private TranslatorHttpBridge() {}

    static void install() {
        HttpHooks.setListener(LISTENER);
    }
}
