package com.song.ui.config;

import com.llm.api.LlmConfig;
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
    private final Button testBtn = new Button("测试连接");
    private final TextField promptField = new TextField();

    private final Label apiKeyLabel = new Label("API Key：");
    private final Label envHintLabel = new Label();
    private final Label baseUrlLabel = new Label("Base URL：");
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
        modelBox.setEditable(false);
        modelBox.setMaxWidth(Double.MAX_VALUE);
        modelBox.setPromptText("请先测试连接");
        promptField.setPromptText("可改写；不保存，下次启动恢复系统默认");

        envHintLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        envHintLabel.setWrapText(true);
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
        apiKeyField.setOnCommitted(key -> {
            if (onApiKeyCommitted != null) {
                onApiKeyCommitted.accept(null);
            }
        });
        refreshEnvHint();
        updatePromptHint();
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
        if (saved != null && !saved.isBlank()) {
            modelBox.getItems().add(saved);
            modelBox.setValue(saved);
        } else {
            modelBox.setValue(null);
        }
        promptField.setText(slot.getPrompt());
        refreshEnvHint();
        updatePromptHint();
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
        String model = modelBox.getValue();
        slot.setModel(model == null ? "" : model.trim());
        String promptText = text(promptField);
        slot.setPrompt(promptText.isEmpty() ? LlmPrompts.DEFAULT_TRANSLATE_SYSTEM : promptText);
    }

    /**
     * 仅把当前 API Key 写入配置（立即生效用）。
     *
     * @param config 应用配置
     */
    public void saveApiKeyOnly(AppConfig config) {
        LlmSlotConfig slot = config.getLlmSlot(vendor);
        if (slot == null) return;
        slot.setApiKey(apiKeyField.getPlain());
        config.setVendor(vendor);
        config.setProvider(ProviderType.defaultFor(vendor));
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
            try {
                List<String> models = LlmFacade.of(cfg).refreshModels();
                Platform.runLater(() -> {
                    applyModels(models);
                    testBtn.setDisable(false);
                    testBtn.setText("测试连接");
                    UiHelper.info("测试连接", "成功，共 " + models.size() + " 个模型");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    testBtn.setText("测试连接");
                    String msg = ex.getMessage() == null ? "未知错误" : ex.getMessage();
                    UiHelper.error("测试连接失败", msg);
                });
            }
        });
    }

    /**
     * 填充下拉并自动选中默认模型（在列表中则选中，否则插入并选中）。
     *
     * @param models 远端模型 id
     */
    private void applyModels(List<String> models) {
        modelBox.getItems().clear();
        if (models != null) {
            modelBox.getItems().addAll(models);
        }
        String preferred = llmVendor.defaultModel();
        if (preferred != null && !preferred.isBlank()) {
            if (!modelBox.getItems().contains(preferred)) {
                modelBox.getItems().add(0, preferred);
            }
            modelBox.setValue(preferred);
        } else if (!modelBox.getItems().isEmpty()) {
            modelBox.setValue(modelBox.getItems().get(0));
        }
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

    private static boolean isDefaultPrompt(String text) {
        if (text == null || text.isBlank()) return true;
        return LlmPrompts.DEFAULT_TRANSLATE_SYSTEM.equals(text.trim());
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
