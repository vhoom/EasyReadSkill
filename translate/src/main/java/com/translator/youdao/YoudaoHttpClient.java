package com.translator.youdao;

import com.translator.http.HttpHooks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * 有道 HTTP 客户端，仅本包可见。
 * 无状态、同步、返回完整字符串；通过 HttpHooks 支持宿主取消。
 */
final class YoudaoHttpClient {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 60000;

    String postForm(String url, Map<String, String> params) {
        HttpURLConnection conn = null;
        boolean opened = false;
        try {
            HttpHooks.checkCancelled();
            String body = formBody(params);
            byte[] bodyBytes = body.getBytes("UTF-8");

            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setFixedLengthStreamingMode(bodyBytes.length);

            HttpHooks.getListener().onOpen(conn);
            opened = true;
            HttpHooks.checkCancelled();

            OutputStream os = conn.getOutputStream();
            try {
                os.write(bodyBytes);
                os.flush();
            } finally {
                os.close();
            }

            int code = conn.getResponseCode();
            InputStream is;
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String respBody = readAll(is);

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + ": " + respBody);
            }
            return respBody;

        } catch (InterruptedIOException e) {
            throw new IllegalStateException("有道请求已中断", e);
        } catch (IOException e) {
            if (HttpHooks.getListener().isCancelled()) {
                throw new IllegalStateException("有道请求已中断", e);
            }
            throw new IllegalStateException("有道请求失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                if (opened) {
                    HttpHooks.getListener().onClose(conn);
                }
                conn.disconnect();
            }
        }
    }

    /** readAll。 */
    private static String readAll(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        try {
            while ((n = is.read(buf)) != -1) {
                HttpHooks.checkCancelled();
                bos.write(buf, 0, n);
            }
        } finally {
            is.close();
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    /** formBody。 */
    private static String formBody(Map<String, String> p) {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = p.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    /** encode。 */
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 不可用", e);
        }
    }
}
