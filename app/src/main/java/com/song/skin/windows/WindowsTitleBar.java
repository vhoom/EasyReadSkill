package com.song.skin.windows;

import com.song.skin.SkinTokens;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Windows 原生标题栏主题适配。
 * 通过 DWM API 设置深色标题栏 / Windows 11 标题栏颜色。
 */
public final class WindowsTitleBar {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsTitleBar.class);

    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;

    private WindowsTitleBar() {
    }

    /** JNA 平台包没有内置 Dwmapi，这里直接动态加载 Windows 的 dwmapi.dll。 */
    private interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(HWND hwnd, int dwAttribute,
                                  Pointer pvAttribute, int cbAttribute);
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static void apply(Stage stage, SkinTokens tokens, boolean dark) {
        if (!isSupported() || stage == null) return;

        try {
            String title = stage.getTitle();
            if (title == null || title.isEmpty()) return;

            HWND hwnd = User32.INSTANCE.FindWindow(null, title);
            if (hwnd == null) {
                LOG.debug("未找到窗口句柄，标题栏主题跳过: {}", title);
                return;
            }

            IntByReference darkValue = new IntByReference(dark ? 1 : 0);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE, darkValue.getPointer(), 4);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, darkValue.getPointer(), 4);

            String caption = dark ? tokens.surfaceAlt() : tokens.surfaceAlt();
            String text = dark ? tokens.text() : tokens.text();
            IntByReference captionRef = new IntByReference(toColorRef(caption));
            IntByReference textRef = new IntByReference(toColorRef(text));
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd,
                    DWMWA_CAPTION_COLOR, captionRef.getPointer(), 4);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd,
                    DWMWA_TEXT_COLOR, textRef.getPointer(), 4);

            LOG.info("Windows 标题栏主题已应用: dark={}", dark);
        } catch (Throwable e) {
            LOG.warn("Windows 标题栏主题应用失败", e);
        }
    }

    /** #RRGGBB -> Windows COLORREF (0x00BBGGRR) */
    private static int toColorRef(String hex) {
        String value = hex == null ? "#000000" : hex.replace("#", "").trim();
        if (value.length() != 6) return 0;
        int r = Integer.parseInt(value.substring(0, 2), 16);
        int g = Integer.parseInt(value.substring(2, 4), 16);
        int b = Integer.parseInt(value.substring(4, 6), 16);
        return (b << 16) | (g << 8) | r;
    }
}