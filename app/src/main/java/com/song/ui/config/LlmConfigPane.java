package com.song.ui.config;

import com.llm.api.LlmConfig;
import com.llm.api.LlmEndpoints;
import com.llm.api.LlmPrompts;
import com.llm.api.LlmVendor;
import com.llm.facade.LlmFacade;
import com.song.config.AppConfig;
import com.song.config.LlmConfigs;
import com.song.config.LlmSlotConfig;
import com.song.model.ProviderType;
import com.song.model.ProviderVendor;
import com.song.service.NetworkWorker;
import com.song.util.UiHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI 兼容大模型厂商配置面板。
 * 模型列表需点「测试连接」拉取；成功后自动选中厂商默认模型。
 */
public class LlmConfigPane extends GridPane {

    private final ProviderVendor vendor;
    private final LlmVendor llmVendor;

    private final MaskedSecretField apiKeyField = new MaskedSecretField();
    private final TextField baseUrlField = new TextField();
    private final ComboBox<String> modelBox = new ComboBox<>();
    private final Button testBtn = new Button("获取模型");
    private final TextField promptField = new TextField();

    private final Label apiKeyLabel = new Label("API Key：");
    private final Label envHintLabel = new Label();
    private final Label baseUrlLabel = new Label("Base URL：");
    private final Label urlHintLabel = new Label();
    private final Label modelLabel = new Label("模型：");
    private final Label promptLabel = new Label("提示词：");
    private final Label promptHintLabel = new Label("系统默认");

    private Consumer<Void> onApiKeyCommitted;

    /**
     * @param vendor UI 厂商（须 {@link ProviderVendor#isLlm()}）
     */
    public LlmConfigPane(ProviderVendor vendor) {
        if (vendor == null || !vendor.isLlm()) {
            throw new IllegalArgumentException("需要 LLM 厂商");
        }
        this.vendor = vendor;
        this.llmVendor = LlmConfigs.toLlmVendor(vendor);

        setHgap(10);
        setVgap(12);
        setPadding(new Insets(4, 0, 4, 0));

        apiKeyField.setPromptText(String.join(" / ", llmVendor.apiKeyEnvs()));
        baseUrlField.setPromptText(llmVendor.defaultBaseUrl());
        // 可编辑：即使拿不到模型列表，也能直接输入模型 id
        modelBox.setEditable(true);
        modelBox.setMaxWidth(Double.MAX_VALUE);
        modelBox.setPromptText("可直接输入模型 id；点「测试连接」拉取列表");
        modelBox.setTooltip(new Tooltip("可直接输入模型 id。点「测试连接」会拉取该账号的模型列表，"
                + "并对选中的模型真发一次对话"));
        promptField.setPromptText("可改写；不保存，下次启动恢复系统默认");

        envHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        envHintLabel.setWrapText(true);
        urlHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        urlHintLabel.setWrapText(true);
        promptHintLabel.setMinWidth(Region.USE_PREF_SIZE);
        promptHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        HBox modelRow = new HBox(8, modelBox, testBtn);
        modelRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(modelBox, Priority.ALWAYS);
        testBtn.setMinWidth(Region.USE_PREF_SIZE);
        testBtn.setOnAction(e -> testConnection());

        int row = 0;
        add(apiKeyLabel, 0, row);
        add(apiKeyField, 1, row++);
        add(envHintLabel, 1, row++);
        add(baseUrlLabel, 0, row);
        add(baseUrlField, 1, row++);
        add(urlHintLabel, 1, row++);
        add(modelLabel, 0, row);
        add(modelRow, 1, row++);
        add(promptLabel, 0, row);
        HBox promptRow = new HBox(8, promptHintLabel, promptField);
        HBox.setHgrow(promptField, Priority.ALWAYS);
        promptField.setMaxWidth(Double.MAX_VALUE);
        add(promptRow, 1, row);

        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        getColumnConstraints().addAll(left, right);

        promptField.textProperty().addListener((obs, o, n) -> updatePromptHint());
        baseUrlField.textProperty().addListener((obs, o, n) -> updateUrlHint());
        apiKeyField.setOnCommitted(key -> {
            if (onApiKeyCommitted != null) {
                onApiKeyCommitted.accept(null);
            }
        });
        refreshEnvHint();
        updatePromptHint();
        updateUrlHint();
    }

    /**
     * API Key 失焦提交后回调（由对话框落盘并刷新 Provider）。
     *
     * @param onApiKeyCommitted 回调
     */
    public void setOnApiKeyCommitted(Consumer<Void> onApiKeyCommitted) {
        this.onApiKeyCommitted = onApiKeyCommitted;
    }

    /** @return 当前面板厂商 */
    public ProviderVendor vendor() {
        return vendor;
    }

    /**
     * 从配置加载。已保存的模型会放入下拉并选中；不预填厂商默认模型。
     *
     * @param config 应用配置
     */
    public void load(AppConfig config) {
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        if (slot == null) return;
        apiKeyField.setPlain(slot.getApiKey());
        baseUrlField.setText(slot.getBaseUrl());
        modelBox.getItems().clear();
        String saved = slot.getModel();
        // 下拉框永远至少有一个条目，避免空列表点不开
        String initial = (saved != null && !saved.isBlank())
                ? saved.trim() : llmVendor.defaultModel();
        if (initial != null && !initial.isBlank()) {
            modelBox.getItems().add(initial);
            modelBox.setValue(initial);
        } else {
            modelBox.setValue(null);
        }
        promptField.setText(slot.getPrompt());
        refreshEnvHint();
        updatePromptHint();
        updateUrlHint();
    }

    /**
     * 写回配置（含 API Key / Base URL / 模型 / 会话提示词）。
     *
     * @param config 应用配置
     */
    public void save(AppConfig config) {
        config.setVendor(vendor);
        config.setProvider(ProviderType.defaultFor(vendor));
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        if (slot == null) return;
        slot.setApiKey(apiKeyField.getPlain());
        slot.setBaseUrl(text(baseUrlField));
        slot.setModel(currentModel());
        String promptText = text(promptField);
        slot.setPrompt(promptText.isEmpty() ? LlmPrompts.DEFAULT_TRANSLATE_SYSTEM : promptText);
    }

    /**
     * 仅把当前 API Key 写入配置（失焦立即生效用）。
     * 不切换当前生效的提供商，避免"只是填个密钥"就把正在用的翻译服务换掉。
     *
     * @param config 应用配置
     */
    public void saveApiKeyOnly(AppConfig config) {
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        if (slot == null) return;
        slot.setApiKey(apiKeyField.getPlain());
    }

    /**
     * 测试连接并拉取模型列表；成功后选中厂商默认模型。
     */
    private void testConnection() {
        String apiKey = resolveApiKey();
        if (apiKey == null) {
            UiHelper.warn("测试连接", "请先填写 API Key，或设置环境变量 "
                    + String.join(" / ", llmVendor.apiKeyEnvs()));
            return;
        }
        String baseUrl = text(baseUrlField);
        if (baseUrl.isBlank()) {
            baseUrl = llmVendor.defaultBaseUrl();
        }
        // 列表请求不依赖具体 model，先占位厂商默认以满足 LlmConfig 校验
        LlmConfig cfg = new LlmConfig(llmVendor, baseUrl, apiKey, llmVendor.defaultModel());

        testBtn.setDisable(true);
        testBtn.setText("连接中…");
        NetworkWorker.submit(() -> {
            List<String> models;
            try {
                models = LlmFacade.of(cfg).refreshModels();
            } catch (Exception ex) {
                finishTest("测试连接失败", "获取模型列表失败：" + message(ex));
                return;
            }

            // 列表能拉通不代表能对话：再真发一次 chat，
            // 否则会出现"测试连接成功，但翻译报 400 model not found"
            String smokeModel = pickSmokeModel(models);
            String chatError = null;
            if (smokeModel != null) {
                try {
                    LlmFacade.of(cfg.withModel(smokeModel))
                            .chat(null, "你是连接测试助手，只回复 OK。", "ping");
                } catch (Exception ex) {
                    chatError = message(ex);
                }
            }
            final String used = smokeModel;
            final String error = chatError;
            Platform.runLater(() -> {
                applyModels(models);
                if (used != null && modelBox.getItems().contains(used)) {
                    modelBox.setValue(used);
                }
                testBtn.setDisable(false);
                testBtn.setText("测试连接");
                if (error == null) {
                    UiHelper.info("测试连接", "成功：模型 " + models.size() + " 个，"
                            + (used == null ? "（列表为空，未做对话测试）" : "对话测试通过（" + used + "）"));
                } else {
                    UiHelper.error("对话测试失败",
                            "模型列表可用（" + models.size() + " 个），但对 " + used + " 发起对话失败：\n"
                                    + error + "\n\n该模型可能不支持 /chat/completions，请换一个模型。");
                }
            });
        });
    }

    private void finishTest(String title, String msg) {
        Platform.runLater(() -> {
            testBtn.setDisable(false);
            testBtn.setText("测试连接");
            UiHelper.error(title, msg);
        });
    }

    private static String message(Exception ex) {
        return ex.getMessage() == null ? "未知错误" : ex.getMessage();
    }

    /** 冒烟测试用哪个模型：当前选中的（仍在列表里）→ 厂商默认（在列表里）→ 列表第一项。 */
    private String pickSmokeModel(List<String> models) {
        String selected = currentModel();
        if (selected != null && !selected.isBlank() && models.contains(selected.trim())) {
            return selected.trim();
        }
        String preferred = llmVendor.defaultModel();
        if (preferred != null && !preferred.isBlank() && models.contains(preferred)) {
            return preferred;
        }
        return models.isEmpty() ? null : models.get(0);
    }

    /**
     * 填充下拉并选中：保留原选择 → 厂商默认（在列表里）→ 列表第一项。
     * 不再把不在列表里的默认模型强插进来，避免选中一个用不了的模型。
     *
     * @param models 远端模型 id
     */
    private void applyModels(List<String> models) {
        String previous = currentModel();
        modelBox.getItems().clear();
        if (models != null) {
            modelBox.getItems().addAll(models);
        }
        String preferred = llmVendor.defaultModel();
        if (previous != null && modelBox.getItems().contains(previous)) {
            modelBox.setValue(previous);
        } else if (preferred != null && !preferred.isBlank()
                && modelBox.getItems().contains(preferred)) {
            modelBox.setValue(preferred);
        } else if (!modelBox.getItems().isEmpty()) {
            modelBox.setValue(modelBox.getItems().get(0));
        } else if (previous != null && !previous.isBlank()) {
            // 列表为空（账号没给模型列表权限等）：保留原值，保证下拉框仍可点开
            modelBox.getItems().add(previous);
            modelBox.setValue(previous);
            modelBox.setPromptText("模型列表为空，可直接输入模型 id");
        } else if (preferred != null && !preferred.isBlank()) {
            modelBox.getItems().add(preferred);
            modelBox.setValue(preferred);
            modelBox.setPromptText("模型列表为空，可直接输入模型 id");
        } else {
            modelBox.setValue(null);
        }
    }

    /** @return 当前模型：优先编辑器里输入的内容（可编辑下拉框），否则取选中项 */
    private String currentModel() {
        if (modelBox.getEditor() != null) {
            String typed = modelBox.getEditor().getText();
            if (typed != null && !typed.isBlank()) {
                return typed.trim();
            }
        }
        String value = modelBox.getValue();
        return value == null ? "" : value.trim();
    }

    /** @return 面板或环境变量中的 API Key；都没有则 null */
    private String resolveApiKey() {
        String key = apiKeyField.getPlain();
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        return LlmConfig.firstEnv(llmVendor.apiKeyEnvs());
    }

    private void refreshEnvHint() {
        String found = LlmConfig.firstEnv(llmVendor.apiKeyEnvs());
        if (found != null) {
            String which = "";
            for (String name : llmVendor.apiKeyEnvs()) {
                String v = System.getenv(name);
                if (v != null && !v.isBlank()) {
                    which = name;
                    break;
                }
            }
            envHintLabel.setText("已从环境变量 " + which + " 读到密钥（" + found.length() + " 字）；失焦后生效");
        } else {
            envHintLabel.setText("未检测到环境变量 "
                    + String.join(" / ", llmVendor.apiKeyEnvs())
                    + "；填写 API Key 失焦后生效");
        }
    }

    private void updatePromptHint() {
        boolean isDefault = isDefaultPrompt(promptField.getText());
        promptHintLabel.setVisible(isDefault);
        promptHintLabel.setManaged(isDefault);
    }

    /** 展示实际会请求的地址，避免 baseUrl 填成完整接口地址后拼出双路径。 */
    private void updateUrlHint() {
        urlHintLabel.setText("实际请求：" + LlmEndpoints.chatUrl(llmVendor, text(baseUrlField)));
    }

    private static boolean isDefaultPrompt(String text) {
        if (text == null || text.isBlank()) return true;
        return LlmPrompts.DEFAULT_TRANSLATE_SYSTEM.equals(text.trim());
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
