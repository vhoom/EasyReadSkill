package com.song.service.factory;

import com.song.service.HttpCalls;
import com.llm.api.LlmHttpHooks;
import com.translator.http.HttpHooks;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * 把主应用的 HttpCalls 接到两个翻译实现的取消钩子，使「停止翻译」能断开进行中的请求：
 * 百度/有道走 {@link HttpHooks}（HttpURLConnection，可 disconnect），
 * 大模型走 {@link LlmHttpHooks}（java.net.http，取消在途 exchange）。
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
        LlmHttpHooks.setListener(HttpCalls::isCancelled);
    }
}
