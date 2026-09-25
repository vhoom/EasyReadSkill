package com.song.config;

import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.model.ScanPath;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {

    /** 配置结构版本：老配置没有该字段，加载时迁移并补写。 */
    public static final int SCHEMA_VERSION = 2;

    private Integer schemaVersion;

    private List<ScanPath> scanPaths = new ArrayList<>();

    /**
     * 扫描路径是否已初始化。默认 true：老配置没有该字段时也视为已初始化，
     * 这样"用户主动清空扫描路径"不会被当成首次安装又把默认目录补回来。
     */
    private boolean scanPathsInitialized = true;

    /** 失效记录是否已归档（一次性维护动作）。 */
    private boolean recordsArchived;
    private ProviderVendor vendor = ProviderVendor.BAIDU;
    private ProviderType provider = ProviderType.getDefault();

    private BaiduConfig baidu = new BaiduConfig();
    private YoudaoConfig youdao = new YoudaoConfig();
    private LlmConfigs llm = new LlmConfigs();

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
        fillDefaultScanPaths();
    }

    /**
     * 仅在"尚未初始化"时填入默认目录。
     * 首次创建的配置由构造函数直接填好；用户清空后不会再被补回来。
     */
    public void ensureDefaultScanPaths() {
        if (scanPaths == null) scanPaths = new ArrayList<>();
        if (scanPathsInitialized) return;
        fillDefaultScanPaths();
    }

    /** 填入默认扫描目录（首次安装用），列表非空时不动。 */
    public void fillDefaultScanPaths() {
        if (scanPaths == null) scanPaths = new ArrayList<>();
        if (!scanPaths.isEmpty()) {
            scanPathsInitialized = true;
            return;
        }
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
            scanPaths.add(new ScanPath(Paths.get(home, name).toString(), false));
        }
        scanPathsInitialized = true;
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

    public LlmConfigs getLlmConfigs() {
        if (llm == null) llm = new LlmConfigs();
        return llm;
    }
    public void setLlmConfigs(LlmConfigs llm) { this.llm = llm; }

    public LlmSlotConfig getLlmSlot(ProviderVendor vendor) {
        return getLlmConfigs().slot(vendor);
    }

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
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) return getYoudaoConfig().getAppId();
        if (v == ProviderVendor.BAIDU) return getBaiduConfig().getAppId();
        return "";
    }
    public void setAppId(String appId) {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) {
            getYoudaoConfig().setAppId(appId);
        } else if (v == ProviderVendor.BAIDU) {
            getBaiduConfig().setAppId(appId);
        }
    }

    public String getApiKey() {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.BAIDU) return getBaiduConfig().getApiKey();
        LlmSlotConfig slot = getLlmSlot(v);
        return slot == null ? "" : slot.getApiKey();
    }
    public void setApiKey(String apiKey) {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.BAIDU) {
            getBaiduConfig().setApiKey(apiKey);
        } else if (v.isLlm()) {
            LlmSlotConfig slot = getLlmSlot(v);
            if (slot != null) slot.setApiKey(apiKey);
        }
    }

    public String getSecretKey() {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) return getYoudaoConfig().getSecretKey();
        if (v == ProviderVendor.BAIDU) return getBaiduConfig().getSecretKey();
        return getApiKey();
    }
    public void setSecretKey(String secretKey) {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) {
            getYoudaoConfig().setSecretKey(secretKey);
        } else if (v == ProviderVendor.BAIDU) {
            getBaiduConfig().setSecretKey(secretKey);
        } else if (v.isLlm()) {
            setApiKey(secretKey);
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
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) return getYoudaoConfig().getPrompt();
        if (v == ProviderVendor.BAIDU) return getBaiduConfig().getPrompt();
        LlmSlotConfig slot = getLlmSlot(v);
        return slot == null ? "" : slot.getPrompt();
    }
    public void setPrompt(String prompt) {
        ProviderVendor v = getVendor();
        if (v == ProviderVendor.YOUDAO) {
            getYoudaoConfig().setPrompt(prompt);
        } else if (v == ProviderVendor.BAIDU) {
            getBaiduConfig().setPrompt(prompt);
        } else if (v.isLlm()) {
            LlmSlotConfig slot = getLlmSlot(v);
            if (slot != null) slot.setPrompt(prompt);
        }
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

    /** @return 配置结构版本；老配置为 null */
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }

    public boolean isScanPathsInitialized() { return scanPathsInitialized; }
    public void setScanPathsInitialized(boolean scanPathsInitialized) {
        this.scanPathsInitialized = scanPathsInitialized;
    }

    /** @return 失效记录是否已归档过 */
    public boolean isRecordsArchived() { return recordsArchived; }
    public void setRecordsArchived(boolean recordsArchived) { this.recordsArchived = recordsArchived; }

    /** 加载时的密钥快照，用来判断"本进程是否动过密钥"。 */
    private transient String secretsAtLoad;

    /** 记下当前密钥快照（加载完成后调用）。 */
    public void markSecretsLoaded() {
        this.secretsAtLoad = secretsFingerprint();
    }

    /**
     * 本进程是否改过密钥。没改过时写盘会采用磁盘上的最新密钥，
     * 避免 app 与 translate 模块（自带测试程序）两个写入者互相覆盖。
     *
     * @return true 表示本进程改过
     */
    public boolean secretsChangedSinceLoad() {
        return secretsAtLoad == null || !secretsAtLoad.equals(secretsFingerprint());
    }

    /**
     * 密钥相关字段的稳定指纹（不依赖 Gson 序列化细节）。
     *
     * @return 指纹字符串
     */
    public String secretsFingerprint() {
        StringBuilder sb = new StringBuilder(256);
        BaiduConfig b = getBaiduConfig();
        sb.append(b.getAppId()).append('|').append(b.getApiKey()).append('|')
                .append(b.getSecretKey()).append('|').append(b.getDomain());
        YoudaoConfig y = getYoudaoConfig();
        sb.append('#').append(y.getAppId()).append('|').append(y.getSecretKey())
                .append('|').append(y.getDomain()).append('|').append(y.getHandleOption());
        for (ProviderVendor v : ProviderVendor.values()) {
            sb.append('#');
            LlmSlotConfig slot = getLlmSlot(v);
            if (slot != null) {
                sb.append(slot.getApiKey()).append('|')
                        .append(slot.getBaseUrl()).append('|').append(slot.getModel());
            }
        }
        return sb.toString();
    }

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

        // 提示词不落盘，启动始终系统默认
        resetLlmPrompts();

        appId = null;
        apiKey = null;
        secretKey = null;
        domain = null;
        youdaoDomain = null;
        handleOption = null;
        prompt = null;
    }

    /** 将百度/有道/大模型 LLM 提示词重置为系统默认（不持久化）。 */
    public void resetLlmPrompts() {
        getBaiduConfig().resetPromptToDefault();
        getYoudaoConfig().resetPromptToDefault();
        getLlmConfigs().resetPrompts();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
