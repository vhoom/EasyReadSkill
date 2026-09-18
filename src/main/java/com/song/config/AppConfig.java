package com.song.config;

import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.model.ScanPath;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private List<ScanPath> scanPaths = new ArrayList<>();
    private ProviderVendor vendor = ProviderVendor.BAIDU;
    private ProviderType provider = ProviderType.getDefault();

    private BaiduConfig baidu = new BaiduConfig();
    private YoudaoConfig youdao = new YoudaoConfig();

    private String targetLang = "zh";
    private String sourceLang = "en";
    private int requestIntervalMs = 500;

    // 上次退出时的界面状态
    private String lastSelectedPath = "";
    private String lastFilter = "UNTRANSLATED";
    private String lastEffect = "OVERWRITE";
    private String skinType = "LIGHT";
    private double windowWidth = 1200;
    private double windowHeight = 780;
    private double windowX = -1;
    private double windowY = -1;
    private boolean windowMaximized = false;

    // 旧版扁平配置字段，仅用于迁移，不再主动使用
    private String appId;
    private String apiKey;
    private String secretKey;
    private String domain;
    private String youdaoDomain;
    private String handleOption;
    private String prompt;

    public AppConfig() {
        ensureDefaultScanPaths();
    }

    public void ensureDefaultScanPaths() {
        if (scanPaths == null) scanPaths = new ArrayList<>();

        String home = System.getProperty("user.home");
        String[] names = {
                ".agents", ".claude", ".codex", ".gemini", ".copilot",
                ".cursor", ".windsurf", ".qwen", ".trae", ".cc-switch",
                ".cline", ".openhands", ".openclaw", ".ovo", ".bob",
                ".reasonix", ".codebuddy", ".lingma", ".kiro", ".minimax",
                ".hermes", ".kimi", ".codemaker", ".codestudio", ".forge",
                ".grok", ".iflow", ".jazz", ".junie", ".pi", ".qoder"
        };

        for (String name : names) {
            String path = Paths.get(home, name).toString();
            boolean exists = scanPaths.stream()
                    .anyMatch(sp -> path.equalsIgnoreCase(sp.getPath()));
            if (!exists) {
                scanPaths.add(new ScanPath(path, false));
            }
        }
    }

    public ProviderVendor getVendor() {
        return vendor != null ? vendor : ProviderVendor.BAIDU;
    }
    public void setVendor(ProviderVendor vendor) { this.vendor = vendor; }

    public ProviderType getProvider() {
        return provider != null ? provider : ProviderType.getDefault();
    }
    public void setProvider(ProviderType provider) { this.provider = provider; }

    public BaiduConfig getBaiduConfig() {
        if (baidu == null) baidu = new BaiduConfig();
        return baidu;
    }
    public void setBaiduConfig(BaiduConfig baidu) { this.baidu = baidu; }

    public YoudaoConfig getYoudaoConfig() {
        if (youdao == null) youdao = new YoudaoConfig();
        return youdao;
    }
    public void setYoudaoConfig(YoudaoConfig youdao) { this.youdao = youdao; }

    public List<ScanPath> getScanPaths() { return scanPaths; }
    public void setScanPaths(List<ScanPath> scanPaths) { this.scanPaths = scanPaths; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public int getRequestIntervalMs() { return requestIntervalMs; }
    public void setRequestIntervalMs(int requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    // ===== 当前生效的 provider 快捷访问 =====

    public String getAppId() {
        return getVendor() == ProviderVendor.YOUDAO
                ? getYoudaoConfig().getAppId()
                : getBaiduConfig().getAppId();
    }
    public void setAppId(String appId) {
        if (getVendor() == ProviderVendor.YOUDAO) {
            getYoudaoConfig().setAppId(appId);
        } else {
            getBaiduConfig().setAppId(appId);
        }
    }

    public String getApiKey() {
        return getVendor() == ProviderVendor.YOUDAO ? "" : getBaiduConfig().getApiKey();
    }
    public void setApiKey(String apiKey) {
        if (getVendor() != ProviderVendor.YOUDAO) {
            getBaiduConfig().setApiKey(apiKey);
        }
    }

    public String getSecretKey() {
        return getVendor() == ProviderVendor.YOUDAO
                ? getYoudaoConfig().getSecretKey()
                : getBaiduConfig().getSecretKey();
    }
    public void setSecretKey(String secretKey) {
        if (getVendor() == ProviderVendor.YOUDAO) {
            getYoudaoConfig().setSecretKey(secretKey);
        } else {
            getBaiduConfig().setSecretKey(secretKey);
        }
    }

    public String getDomain() {
        return getBaiduConfig().getDomain();
    }
    public void setDomain(String domain) { getBaiduConfig().setDomain(domain); }

    public String getYoudaoDomain() {
        return getYoudaoConfig().getDomain();
    }
    public void setYoudaoDomain(String youdaoDomain) {
        getYoudaoConfig().setDomain(youdaoDomain);
    }

    public String getHandleOption() {
        return getYoudaoConfig().getHandleOption();
    }
    public void setHandleOption(String handleOption) {
        getYoudaoConfig().setHandleOption(handleOption);
    }

    public String getPrompt() {
        return getYoudaoConfig().getPrompt();
    }
    public void setPrompt(String prompt) {
        getYoudaoConfig().setPrompt(prompt);
    }

    public String getLastSelectedPath() { return lastSelectedPath; }
    public void setLastSelectedPath(String lastSelectedPath) { this.lastSelectedPath = lastSelectedPath; }

    public String getLastFilter() { return lastFilter; }
    public void setLastFilter(String lastFilter) { this.lastFilter = lastFilter; }

    public String getLastEffect() { return lastEffect; }
    public void setLastEffect(String lastEffect) { this.lastEffect = lastEffect; }

    public String getSkinType() { return skinType; }
    public void setSkinType(String skinType) { this.skinType = skinType; }

    public double getWindowWidth() { return windowWidth; }
    public void setWindowWidth(double windowWidth) { this.windowWidth = windowWidth; }

    public double getWindowHeight() { return windowHeight; }
    public void setWindowHeight(double windowHeight) { this.windowHeight = windowHeight; }

    public double getWindowX() { return windowX; }
    public void setWindowX(double windowX) { this.windowX = windowX; }

    public double getWindowY() { return windowY; }
    public void setWindowY(double windowY) { this.windowY = windowY; }

    public boolean isWindowMaximized() { return windowMaximized; }
    public void setWindowMaximized(boolean windowMaximized) { this.windowMaximized = windowMaximized; }

    /** 将旧版 config.json 中的扁平字段迁移到各自 provider 配置。 */
    public void migrateLegacy() {
        getBaiduConfig();
        getYoudaoConfig();

        if (isBlank(getBaiduConfig().getAppId())) getBaiduConfig().setAppId(appId);
        if (isBlank(getBaiduConfig().getApiKey())) getBaiduConfig().setApiKey(apiKey);
        if (isBlank(getBaiduConfig().getSecretKey())) getBaiduConfig().setSecretKey(secretKey);
        if (isBlank(getBaiduConfig().getDomain())) getBaiduConfig().setDomain(domain);

        if (isBlank(getYoudaoConfig().getAppId())) getYoudaoConfig().setAppId(appId);
        if (isBlank(getYoudaoConfig().getSecretKey())) getYoudaoConfig().setSecretKey(secretKey);
        if (isBlank(getYoudaoConfig().getDomain())) getYoudaoConfig().setDomain(youdaoDomain);
        if (isBlank(getYoudaoConfig().getHandleOption())) getYoudaoConfig().setHandleOption(handleOption);
        if (isBlank(getYoudaoConfig().getPrompt())) getYoudaoConfig().setPrompt(prompt);

        appId = null;
        apiKey = null;
        secretKey = null;
        domain = null;
        youdaoDomain = null;
        handleOption = null;
        prompt = null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}