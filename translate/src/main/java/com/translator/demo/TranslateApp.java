package com.translator.demo;

import com.translator.api.TranslateRequest;
import com.translator.api.TranslateResponse;
import com.translator.baidu.BaiduConfig;
import com.translator.bootstrap.TranslatorBootstrap;
import com.translator.config.CredentialPaths;
import com.translator.config.CredentialStore;
import com.translator.config.StoredCredentials;
import com.translator.facade.TranslateService;
import com.translator.youdao.YoudaoConfig;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 翻译测试工具（JavaFX）。
 * - 顶部：密钥配置区
 * - 中部：厂商 / 能力选择 + 源/目标语言
 * - 输入框 + 翻译按钮 + 输出框
 * - 状态栏：耗时 / 错误
 */
public class TranslateApp extends Application {

    private static final String[] YOUDAO_FROM = {
            "auto", "zh-CHS", "en", "ja", "ko", "fr", "de", "ru"
    };
    private static final String[] YOUDAO_TO = {
            "en", "zh-CHS", "ja", "ko", "fr", "de", "ru"
    };
    /** 百度语种码与有道不同：zh / jp / kor / fra */
    private static final String[] BAIDU_FROM = {
            "auto", "zh", "en", "jp", "kor", "fra", "de", "ru"
    };
    private static final String[] BAIDU_TO = {
            "en", "zh", "jp", "kor", "fra", "de", "ru"
    };

    // ===== 密钥输入 =====
    private TextField youdaoAppKeyField;
    private PasswordField youdaoAppSecretField;
    private TextField baiduAppIdField;
    private PasswordField baiduSecretField;

    // ===== 参数选择 =====
    private ComboBox<String> vendorBox;
    private ComboBox<String> apiBox;
    private ComboBox<String> fromBox;
    private ComboBox<String> toBox;
    private TextField domainField;
    private TextField promptField;

    // ===== 输入输出 =====
    private TextArea inputArea;
    private TextArea outputArea;
    private Button translateBtn;
    private Label statusLabel;

    @Override
    /** start。 */
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        root.setTop(buildConfigPane());
        root.setCenter(buildMainPane());
        root.setBottom(buildStatusBar());

        loadCredentialsIntoUi();

        stage.setTitle("Translator 测试工具");
        stage.setScene(new Scene(root, 860, 640));
        stage.show();
    }

    /** buildConfigPane。 */
    private TitledPane buildConfigPane() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(8));

        grid.add(new Label("有道 appKey:"), 0, 0);
        youdaoAppKeyField = new TextField();
        youdaoAppKeyField.setPromptText("YOUR_APP_KEY");
        youdaoAppKeyField.setPrefWidth(220);
        grid.add(youdaoAppKeyField, 1, 0);

        grid.add(new Label("有道 appSecret:"), 2, 0);
        youdaoAppSecretField = new PasswordField();
        youdaoAppSecretField.setPromptText("YOUR_APP_SECRET");
        youdaoAppSecretField.setPrefWidth(220);
        grid.add(youdaoAppSecretField, 3, 0);

        grid.add(new Label("百度 appId:"), 0, 1);
        baiduAppIdField = new TextField();
        baiduAppIdField.setPromptText("YOUR_APP_ID");
        baiduAppIdField.setPrefWidth(220);
        grid.add(baiduAppIdField, 1, 1);

        grid.add(new Label("百度 secret:"), 2, 1);
        baiduSecretField = new PasswordField();
        baiduSecretField.setPromptText("YOUR_SECRET");
        baiduSecretField.setPrefWidth(220);
        grid.add(baiduSecretField, 3, 1);

        Button reloadBtn = new Button("从配置加载");
        reloadBtn.setOnAction(e -> loadCredentialsIntoUi());
        Button saveBtn = new Button("保存到配置");
        saveBtn.setOnAction(e -> saveCredentialsFromUi());
        HBox actions = new HBox(8, reloadBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        grid.add(actions, 0, 2, 4, 1);

        TitledPane pane = new TitledPane(
                "密钥（~/.easyReadSkill/config.json，与主应用共用）", grid);
        pane.setCollapsible(true);
        pane.setExpanded(true);
        return pane;
    }

    /** loadCredentialsIntoUi。 */
    private void loadCredentialsIntoUi() {
        StoredCredentials creds = CredentialStore.load();
        youdaoAppKeyField.setText(nullToEmpty(creds.getYoudao().getAppId()));
        youdaoAppSecretField.setText(nullToEmpty(creds.getYoudao().getSecretKey()));
        baiduAppIdField.setText(nullToEmpty(creds.getBaidu().getAppId()));
        String baiduSecret = StoredCredentials.resolveBaiduSecret(creds.getBaidu());
        baiduSecretField.setText(nullToEmpty(baiduSecret));

        if (isBlank(domainField.getText()) && !isBlank(creds.baiduDomainOrEmpty())) {
            domainField.setText(creds.baiduDomainOrEmpty());
        }
        if (isBlank(promptField.getText()) && !isBlank(creds.youdaoPromptOrEmpty())) {
            promptField.setText(creds.youdaoPromptOrEmpty());
        }
        statusLabel.setText("已加载 " + CredentialPaths.configFile());
    }

    /** saveCredentialsFromUi。 */
    private void saveCredentialsFromUi() {
        try {
            StoredCredentials creds = CredentialStore.load();
            CredentialStore.applyUiKeys(creds,
                    youdaoAppKeyField.getText(),
                    youdaoAppSecretField.getText(),
                    baiduAppIdField.getText(),
                    baiduSecretField.getText());
            if (!isBlank(domainField.getText())) {
                creds.getBaidu().setDomain(domainField.getText().trim());
            }
            if (!isBlank(promptField.getText())) {
                creds.getYoudao().setPrompt(promptField.getText().trim());
            }
            CredentialStore.save(creds);
            statusLabel.setText("已保存 " + CredentialPaths.configFile());
        } catch (Exception ex) {
            statusLabel.setText("保存失败: " + ex.getMessage());
        }
    }

    /** nullToEmpty。 */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** buildMainPane。 */
    private VBox buildMainPane() {
        vendorBox = new ComboBox<String>();
        vendorBox.getItems().addAll("youdao", "baidu");
        vendorBox.setValue("baidu");

        apiBox = new ComboBox<String>();

        fromBox = new ComboBox<String>();
        fromBox.setEditable(true);

        toBox = new ComboBox<String>();
        toBox.setEditable(true);

        domainField = new TextField();
        domainField.setPromptText("百度领域：it / finance / senimed / academic ...");
        domainField.setPrefWidth(200);

        promptField = new TextField();
        promptField.setPromptText("大模型指令：如 使用学术风格来翻译");
        promptField.setPrefWidth(260);

        vendorBox.valueProperty().addListener((obs, o, n) -> onVendorChanged());
        apiBox.valueProperty().addListener((obs, o, n) -> onApiChanged());
        onVendorChanged();

        HBox row1 = new HBox(8,
                new Label("厂商:"), vendorBox,
                new Label("能力:"), apiBox,
                new Label("源:"), fromBox,
                new Label("目标:"), toBox);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(8, 8, 0, 8));

        HBox row2 = new HBox(8,
                new Label("领域:"), domainField,
                new Label("指令:"), promptField);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(4, 8, 8, 8));

        inputArea = new TextArea();
        inputArea.setPromptText("请输入待翻译文本...");
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(6);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(6);
        outputArea.setPromptText("翻译结果...");

        translateBtn = new Button("翻译");
        translateBtn.setDefaultButton(true);
        translateBtn.setOnAction(e -> onTranslate());

        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> {
            inputArea.clear();
            outputArea.clear();
            statusLabel.setText("就绪");
        });

        HBox btnBar = new HBox(8, translateBtn, clearBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(4, 8, 4, 8));

        VBox center = new VBox(4,
                new Label("原文:"), inputArea,
                new Label("译文:"), outputArea,
                btnBar);
        VBox.setVgrow(inputArea, Priority.ALWAYS);
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        center.setPadding(new Insets(0, 8, 0, 8));

        return new VBox(4, row1, row2, center);
    }

    /** onVendorChanged。 */
    private void onVendorChanged() {
        String vendor = vendorBox.getValue();
        boolean baidu = "baidu".equals(vendor);

        String prevApi = apiBox.getValue();
        apiBox.getItems().clear();
        if (baidu) {
            apiBox.getItems().addAll("nmt", "llm", "domain");
        } else {
            apiBox.getItems().addAll("nmt", "llm");
        }
        if (prevApi != null && apiBox.getItems().contains(prevApi)) {
            apiBox.setValue(prevApi);
        } else {
            apiBox.setValue("nmt");
        }

        refillLang(fromBox, baidu ? BAIDU_FROM : YOUDAO_FROM, baidu ? "auto" : "auto");
        refillLang(toBox, baidu ? BAIDU_TO : YOUDAO_TO, baidu ? "en" : "en");
        onApiChanged();
    }

    /** onApiChanged。 */
    private void onApiChanged() {
        String vendor = vendorBox.getValue();
        String api = apiBox.getValue();
        boolean needDomain = "baidu".equals(vendor) && "domain".equals(api);
        boolean needPrompt = "llm".equals(api);
        domainField.setDisable(!needDomain);
        promptField.setDisable(!needPrompt);
    }

    /** refillLang。 */
    private static void refillLang(ComboBox<String> box, String[] items, String def) {
        String prev = box.getValue();
        box.getItems().setAll(items);
        if (prev != null && box.getItems().contains(prev)) {
            box.setValue(prev);
        } else {
            box.setValue(def);
        }
    }

    /** buildStatusBar。 */
    private HBox buildStatusBar() {
        statusLabel = new Label("就绪");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    /** onTranslate。 */
    private void onTranslate() {
        final String text = inputArea.getText();
        if (text == null || text.trim().isEmpty()) {
            statusLabel.setText("请输入文本");
            return;
        }

        final String vendor = vendorBox.getValue();
        final String api = apiBox.getValue();
        final String from = fromBox.getValue();
        final String to = toBox.getValue();
        final String domain = domainField.getText();
        final String prompt = promptField.getText();

        final String youdaoKey = youdaoAppKeyField.getText();
        final String youdaoSecret = youdaoAppSecretField.getText();
        final String baiduId = baiduAppIdField.getText();
        final String baiduSecret = baiduSecretField.getText();

        if ("youdao".equals(vendor)) {
            if (isBlank(youdaoKey) || isBlank(youdaoSecret)) {
                statusLabel.setText("请填写有道 appKey / appSecret");
                return;
            }
        } else {
            if (isBlank(baiduId) || isBlank(baiduSecret)) {
                statusLabel.setText("请填写百度 appId / secret");
                return;
            }
        }
        if ("domain".equals(api) && isBlank(domain)) {
            statusLabel.setText("领域翻译请填写 domain（如 it / senimed）");
            return;
        }

        translateBtn.setDisable(true);
        outputArea.setText("翻译中...");
        statusLabel.setText("请求中...");
        final long t0 = System.currentTimeMillis();

        Task<TranslateResponse> task = new Task<TranslateResponse>() {
            @Override
            /** call。 */
            protected TranslateResponse call() {
                YoudaoConfig youdao = null;
                BaiduConfig baidu = null;
                if ("youdao".equals(vendor)) {
                    youdao = new YoudaoConfig(youdaoKey, youdaoSecret);
                } else {
                    baidu = new BaiduConfig(baiduId, baiduSecret);
                }
                TranslateService service = TranslatorBootstrap.create(youdao, baidu);

                TranslateRequest.Builder b = TranslateRequest.builder()
                        .vendor(vendor).api(api)
                        .text(text).from(from).to(to);
                if (!isBlank(domain)) {
                    b.domain(domain.trim());
                }
                if (!isBlank(prompt)) {
                    b.prompt(prompt.trim());
                }
                if ("baidu".equals(vendor) && "llm".equals(api)) {
                    b.model("llm");
                }
                return service.translate(b.build());
            }
        };

        task.setOnSucceeded(e -> {
            TranslateResponse resp = task.getValue();
            outputArea.setText(resp.getText());
            long cost = System.currentTimeMillis() - t0;
            statusLabel.setText("成功  " + cost + " ms"
                    + (resp.getLangType() == null ? "" : "  " + resp.getLangType()));
            translateBtn.setDisable(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            outputArea.setText("失败: " + (ex == null ? "未知错误" : ex.getMessage()));
            long cost = System.currentTimeMillis() - t0;
            statusLabel.setText("失败  " + cost + " ms");
            translateBtn.setDisable(false);
        });

        Thread t = new Thread(task, "translate-worker");
        t.setDaemon(true);
        t.start();
    }

    /** 是否Blank。 */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 程序入口。 */
    public static void main(String[] args) {
        launch(args);
    }
}
