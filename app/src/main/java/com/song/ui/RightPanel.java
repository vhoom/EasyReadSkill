package com.song.ui;

import com.song.model.*;
import com.song.model.EffectType;
import com.song.model.SkillFile;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import com.song.config.ConfigManager;
import com.song.skin.SkinManager;
import com.song.service.HttpCalls;
import com.song.service.NetworkWorker;
import com.song.service.SkillFileService;
import com.song.service.SkillParser;
import com.song.service.TranslationResult;
import com.song.util.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class RightPanel extends VBox {

    private static final Logger LOG = LoggerFactory.getLogger(RightPanel.class);

    /** 原文与备份不一致时用户的选择。 */
    private enum ConflictChoice { REBACKUP, KEEP_BACKUP, SKIP }

    private final AppState state;

    private final Label originalLabel = new Label("备份数据：");
    private final TextArea originalArea = new TextArea();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Label translatedLabel = new Label("实际读取数据：");
    private final Label backupTimeLabel = new Label("翻译更新时间：--");
    private final TextArea translatedArea = new TextArea();

    private final ToggleGroup effectGroup = new ToggleGroup();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("");
    private final Button stopButton = new Button("中断翻译");

    private volatile boolean stopFlag = false;
    private volatile Future<?> translateJob;

    public RightPanel(AppState state) {
        this.state = state;
        setSpacing(0);
        setPadding(new Insets(20, 24, 16, 24));
        getStyleClass().add("right-panel");

        originalLabel.getStyleClass().add("eyebrow");
        originalArea.setEditable(false);
        originalArea.setWrapText(true);

        backupTimeLabel.getStyleClass().add("secondary");

        translatedLabel.getStyleClass().add("eyebrow");
        translatedLabel.setVisible(true);
        translatedArea.setEditable(false);
        translatedArea.setWrapText(true);
        translatedArea.setVisible(true);

        Label effectLabel = new Label("翻译效果");
        effectLabel.getStyleClass().add("eyebrow");
        HBox effectBox = new HBox(12);
        for (EffectType et : EffectType.values()) {
            RadioButton rb = new RadioButton(et.getLabel());
            rb.setToggleGroup(effectGroup);
            rb.setUserData(et);
            rb.setTooltip(new Tooltip(effectHint(et)));
            if (et == state.getEffect())
                rb.setSelected(true);
            rb.selectedProperty().addListener((obs, o, n) -> {
                if (n)
                    state.effectProperty().set(et);
            });
            effectBox.getChildren().add(rb);
        }

        Button retranslate = new Button("重新翻译");
        retranslate.setTooltip(new Tooltip("忽略翻译记忆，强制请求翻译 API"));
        Button transCur = new Button("翻译当前文件");
        transCur.setTooltip(new Tooltip("只翻译当前选中的文件；原文与翻译记忆一致时直接用缓存"));
        Button transSel = new Button("翻译勾选文件");
        transSel.getStyleClass().add("primary");
        transSel.setTooltip(new Tooltip("翻译左侧所有勾选的文件（包含被筛选隐藏的）"));
        Button restore = new Button("还原成备份");
        restore.setTooltip(new Tooltip("把文件内容改回原始备份"));
        Button rebackup = new Button("重新备份");
        rebackup.setTooltip(new Tooltip("把当前文件内容登记为新的原始备份，旧备份会留档"));
        Button markTranslated = new Button("标记为已翻译");
        markTranslated.setTooltip(new Tooltip("文件内容不动，只登记为已翻译；原文能从内容相同的记录里采用"));
        Button clearBackup = new Button("清空全部备份");
        clearBackup.setTooltip(new Tooltip(
                "清空整个备份库（所有文件的备份与译文记录）；技能文件不动，清空前自动备份 records.json"));
        stopButton.setDisable(true);

        // 按钮标题直接显示作用范围：勾选优先，没有勾选时作用于当前文件
        bindScope(retranslate, "重新翻译");
        bindScope(restore, "还原成备份");
        bindScope(rebackup, "重新备份");
        bindScope(markTranslated, "标记为已翻译");
        bindCheckedCount(transSel, "翻译勾选文件");

        retranslate.setOnAction(e -> retranslateCurrent());
        transCur.setOnAction(e -> translateCurrent());
        transSel.setOnAction(e -> translateSelected());
        restore.setOnAction(e -> restoreCurrent());
        rebackup.setOnAction(e -> rebackupCurrent());
        markTranslated.setOnAction(e -> markTranslatedCurrent());
        clearBackup.setOnAction(e -> clearBackupCurrent());
        stopButton.setOnAction(e -> {
            stopFlag = true;
            HttpCalls.cancelAll();
            Future<?> job = translateJob;
            if (job != null) {
                job.cancel(true);
            }
        });

        FlowPane btnBox = new FlowPane(8, 8, transSel, transCur, retranslate, stopButton,
                restore, rebackup, markTranslated, clearBackup);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        HBox progBox = new HBox(8, progressBar, progressLabel);
        progBox.setAlignment(Pos.CENTER_LEFT);
        setProgressVisible(false);

        VBox reading = new VBox(8, originalLabel, originalArea, backupTimeLabel, translatedLabel, translatedArea);
        VBox.setVgrow(originalArea, Priority.ALWAYS);
        VBox.setVgrow(translatedArea, Priority.ALWAYS);
        VBox.setVgrow(reading, Priority.ALWAYS);

        VBox actionBar = new VBox(12, effectLabel, effectBox, btnBox, progBox);
        actionBar.getStyleClass().add("action-bar");
        actionBar.setPadding(new Insets(16, 0, 0, 0));

        getChildren().addAll(reading, actionBar);

        state.selectedFileProperty().addListener((obs, o, n) -> showFile(n));
        state.versionProperty().addListener((obs, o, n) -> showFile(state.getSelectedFile()));
    }

    private static String effectHint(EffectType effect) {
        return switch (effect) {
            case OVERWRITE -> "译文直接覆盖原 Description";
            case KEEP_ENGLISH -> "译文与原文同时写入，格式为“译文/原文”";
        };
    }

    private void bindScope(Button button, String base) {
        state.selectedCountProperty().addListener((obs, o, n) -> updateScopeText(button, base));
        updateScopeText(button, base);
    }

    private void updateScopeText(Button button, String base) {
        int checked = state.selectedCountProperty().get();
        button.setText(checked > 0 ? base + "（勾选 " + checked + "）" : base + "（当前）");
    }

    private void bindCheckedCount(Button button, String base) {
        state.selectedCountProperty().addListener((obs, o, n) -> updateCheckedText(button, base));
        updateCheckedText(button, base);
    }

    private void updateCheckedText(Button button, String base) {
        int checked = state.selectedCountProperty().get();
        button.setText(checked > 0 ? base + " (" + checked + ")" : base);
        button.setDisable(checked == 0);
    }

    private void showFile(SkillFile sf) {
        if (sf == null) {
            originalLabel.setText("备份数据：");
            originalArea.clear();
            backupTimeLabel.setText("翻译更新时间：--");
            translatedLabel.setText("实际读取数据：");
            translatedArea.clear();
            return;
        }

        // 上块：备份数据
        TranslationRecord rec = state.getRecordManager().get(sf.getFilePath());
        String backedUp = rec != null ? rec.getOriginalDescription() : null;
        if (backedUp != null && !backedUp.isEmpty()) {
            originalLabel.setText("备份数据：");
            originalArea.setText(backedUp);
        } else {
            originalLabel.setText("备份数据（尚未备份）：");
            originalArea.setText("（当前文件还没有备份数据，翻译时会自动备份）");
        }

        long lastTranslate = rec != null ? rec.getLastTranslateTimestamp() : 0L;
        if (lastTranslate > 0) {
            backupTimeLabel.setText("翻译更新时间："
                    + TIME_FORMATTER.format(Instant.ofEpochMilli(lastTranslate)));
        } else {
            backupTimeLabel.setText("翻译更新时间：--");
        }

        // 下块：实际读取数据
        String current;
        try {
            String content = SkillFileService.readFile(sf.getFilePath());
            String desc = SkillParser.extractDescription(content);
            current = desc == null ? "（未读取到 Description 字段）" : desc;
        } catch (IOException e) {
            current = "（读取失败：" + e.getMessage() + "）";
        }

        if (sf.getStatus() == TranslateStatus.TRANSLATED) {
            translatedLabel.setText("实际读取数据（已翻译）：");
        } else {
            translatedLabel.setText("实际读取数据：");
        }
        translatedArea.setText(current);
    }

    private List<SkillFile> getCheckedFiles() {
        List<SkillFile> checked = new ArrayList<>();
        for (SkillFile sf : state.getAllFiles()) {
            if (sf.isSelected())
                checked.add(sf);
        }
        return checked;
    }

    /** 勾选优先，没有勾选则作用于当前选中项（按钮标题会显示当前作用范围）。 */
    private List<SkillFile> getActionTargets() {
        List<SkillFile> checked = getCheckedFiles();
        if (!checked.isEmpty())
            return checked;

        SkillFile current = state.getSelectedFile();
        if (current == null)
            return List.of();
        return List.of(current);
    }

    private void retranslateCurrent() {
        List<SkillFile> targets = getActionTargets();
        if (targets.isEmpty()) {
            UiHelper.warn("提示", "请先勾选文件或在左侧选择一个文件。");
            return;
        }
        if (!UiHelper.confirm("确认",
                "重新翻译会忽略翻译记忆缓存，强制请求翻译 API。\n"
                        + describeScope(targets)
                        + "，是否继续？")) {
            return;
        }
        startTranslate(targets, false);
    }

    private void translateCurrent() {
        SkillFile sf = state.getSelectedFile();
        if (sf == null) {
            UiHelper.warn("提示", "请先在左侧选择一个文件。");
            return;
        }
        if (state.getFileService().hasUnknownOriginal(sf.getFilePath())) {
            if (!UiHelper.confirm("确认",
                    "该文件被标记为已翻译，但没有原始备份。\n"
                            + "继续会把当前内容当作原文来翻译（可能变成二次翻译）。\n"
                            + "建议先用「重新备份」登记原文。是否继续？")) {
                return;
            }
        } else if (sf.getStatus() == TranslateStatus.TRANSLATED) {
            if (!UiHelper.confirm("确认",
                    "该文件已经翻译过。\n"
                            + "原文与翻译记忆一致时会直接使用缓存，不再请求 API；\n"
                            + "需要强制重新请求请用「重新翻译」。是否继续？")) {
                return;
            }
        }
        startTranslate(List.of(sf), true);
    }

    private void translateSelected() {
        List<SkillFile> selected = getCheckedFiles();
        if (selected.isEmpty()) {
            UiHelper.warn("提示", "没有勾选任何文件。");
            return;
        }
        long already = selected.stream()
                .filter(f -> f.getStatus() == TranslateStatus.TRANSLATED).count();
        long failed = selected.stream()
                .filter(f -> f.getStatus() == TranslateStatus.FAILED).count();
        long noBackup = selected.stream()
                .filter(f -> state.getFileService().hasUnknownOriginal(f.getFilePath())).count();
        if (already > 0 || failed > 0 || noBackup > 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("本次将处理勾选的 ").append(selected.size()).append(" 个文件。\n");
            if (already > 0) {
                msg.append("其中 ").append(already)
                        .append(" 个已翻译过：原文与翻译记忆一致时直接用缓存，不请求 API。\n");
            }
            if (failed > 0) {
                msg.append("其中 ").append(failed)
                        .append(" 个上次失败或未读到 Description，会再试一次。\n");
            }
            if (noBackup > 0) {
                msg.append("其中 ").append(noBackup)
                        .append(" 个没有原始备份（曾被标记为已翻译），会把当前内容当原文翻译。\n");
            }
            msg.append("是否继续？");
            if (!UiHelper.confirm("确认", msg.toString())) {
                return;
            }
        }
        startTranslate(selected, true);
    }

    private String describeScope(List<SkillFile> targets) {
        if (state.selectedCountProperty().get() > 0) {
            return "本次作用于勾选的 " + targets.size() + " 个文件";
        }
        return "本次作用于当前选中的 1 个文件";
    }

    private void startTranslate(List<SkillFile> targets, boolean useCache) {
        Future<?> job = translateJob;
        if (job != null && !job.isDone()) {
            UiHelper.warn("提示", "已有翻译任务正在运行。");
            return;
        }
        HttpCalls.arm();
        stopFlag = false;
        stopButton.setDisable(false);
        EffectType effect = state.getEffect();
        String from = state.getConfig().getSourceLang();
        String to = state.getConfig().getTargetLang();

        progressBar.setProgress(0);
        progressLabel.setText("0/" + Math.max(1, targets.size()));
        setProgressVisible(true);
        log("开始翻译 " + targets.size() + " 个文件（network 单线程）"
                + "，间隔：" + state.getConfig().getRequestIntervalMs() + "ms"
                + "，效果：" + effect.getLabel()
                + "，批量：" + state.getTranslationService().maxBatchItems() + " 段/次");

        translateJob = NetworkWorker.submit(() -> {
            int ok = 0;
            int fail = 0;
            int stopped = 0;
            int skipped = 0;
            List<String> errors = new ArrayList<>();

            // 预检：哪些文件的原文已经和备份对不上（技能被更新或被别的工具改过）
            Set<String> conflictPaths = new HashSet<>();
            List<String> conflictNames = new ArrayList<>();
            for (SkillFile sf : targets) {
                if (halted()) break;
                if (state.getFileService().hasConflict(sf.getFilePath())) {
                    conflictPaths.add(sf.getFilePath());
                    conflictNames.add(sf.getParentName());
                }
            }
            ConflictChoice choice = ConflictChoice.KEEP_BACKUP;
            if (!conflictPaths.isEmpty()) {
                choice = askConflictChoice(conflictNames);
                log("原文已变更 " + conflictPaths.size() + " 个文件，用户选择：" + choiceLabel(choice));
            }

            Set<String> refreshSet = new HashSet<>();
            Set<String> skipPaths = new HashSet<>();
            if (choice == ConflictChoice.REBACKUP) {
                refreshSet.addAll(conflictPaths);
            } else if (choice == ConflictChoice.SKIP) {
                skipPaths.addAll(conflictPaths);
            }

            Map<String, SkillFile> byPath = new HashMap<>();
            List<String> paths = new ArrayList<>();
            for (SkillFile sf : targets) {
                byPath.put(sf.getFilePath(), sf);
                if (skipPaths.contains(sf.getFilePath())) {
                    skipped++;
                    log(sf.getParentName() + " 跳过（原文已变更）");
                } else {
                    paths.add(sf.getFilePath());
                }
            }
            final int total = paths.size();

            // 真正干活：单文件逻辑仍在 SkillFileService 里，
            // 大模型会把几段原文拼成一次对话再拆回（见 translateFiles）
            List<TranslationResult> results = total == 0 ? List.of()
                    : state.getFileService().translateFiles(paths, effect, from, to, useCache,
                            refreshSet,
                            done -> Platform.runLater(() -> {
                                progressBar.setProgress((double) done / total);
                                progressLabel.setText(done + "/" + total);
                                state.bumpVersion();
                            }));

            for (int i = 0; i < total; i++) {
                SkillFile sf = byPath.get(paths.get(i));
                TranslationResult result = results.get(i);
                if (sf == null) {
                    continue;
                }
                if (result.isInterrupted()) {
                    stopped++;
                    continue;
                }
                if (result.isConflict()) {
                    skipped++;
                    log(sf.getParentName() + " 原文已变更，已跳过");
                    continue;
                }
                if (result.isSuccess()) {
                    ok++;
                    sf.setStatus(TranslateStatus.TRANSLATED);
                    TranslationRecord rec = state.getRecordManager().get(sf.getFilePath());
                    if (rec != null) {
                        sf.setTranslatedDescription(rec.getTranslatedDescription());
                    }
                } else {
                    fail++;
                    sf.setStatus(TranslateStatus.FAILED);
                    String error = result.getErrorMessage() == null
                            ? "翻译失败" : result.getErrorMessage();
                    errors.add(sf.getParentName() + "：" + error);
                    log(sf.getParentName() + " 失败：" + error);
                }
            }

            state.getRecordManager().flush();

            final int fok = ok;
            final int ffail = fail;
            final int fstop = stopped;
            final int fskip = skipped;
            final boolean wasHalted = stopFlag || fstop > 0;
            final List<String> finalErrors = List.copyOf(errors);
            Platform.runLater(() -> {
                log(String.format("完成：成功 %d，失败 %d，跳过 %d，中断 %d", fok, ffail, fskip, fstop));
                stopButton.setDisable(true);
                progressLabel.setText(wasHalted ? "已中断" : "完成");
                setProgressVisible(false);
                state.bumpVersion();
                showFile(state.getSelectedFile());
                UiHelper.info(wasHalted ? "已中断" : "翻译完成",
                        "成功 " + fok + "，失败 " + ffail
                                + (fskip > 0 ? "，跳过 " + fskip : "")
                                + (wasHalted ? "，未完成 " + fstop : ""));
                if (!finalErrors.isEmpty()) {
                    UiHelper.error("翻译失败", String.join("\n", finalErrors));
                }
            });
        });
    }

    private static boolean halted() {
        return HttpCalls.isCancelled() || Thread.currentThread().isInterrupted();
    }

    private static String choiceLabel(ConflictChoice choice) {
        return switch (choice) {
            case REBACKUP -> "按当前文件重新备份";
            case KEEP_BACKUP -> "沿用旧备份";
            case SKIP -> "跳过这些文件";
        };
    }

    /**
     * 原文与备份不一致时问一次，给三个按钮。在 network 线程上等待，
     * 对话框本身跑在 JavaFX 线程上。
     *
     * @param names 冲突文件名（用于提示）
     * @return 用户的选择
     */
    private ConflictChoice askConflictChoice(List<String> names) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ConflictChoice> picked = new AtomicReference<>(ConflictChoice.SKIP);
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("原文已变更");
                alert.setHeaderText(null);
                int shown = Math.min(3, names.size());
                String sample = String.join("、", names.subList(0, shown));
                alert.setContentText("有 " + names.size() + " 个文件的描述与上次备份不一致"
                        + "（技能被更新或被其他工具改过）。\n"
                        + "例如：" + sample + (names.size() > shown ? " 等" : "") + "\n\n"
                        + "· 按当前文件重新备份：把现在的内容当作原文，之后可还原到这里\n"
                        + "· 沿用旧备份：继续用旧原文翻译，会覆盖当前内容\n"
                        + "· 跳过这些文件：本次不处理它们");
                ButtonType rebackup = new ButtonType("按当前文件重新备份", ButtonBar.ButtonData.OK_DONE);
                ButtonType keep = new ButtonType("沿用旧备份", ButtonBar.ButtonData.NO);
                ButtonType skip = new ButtonType("跳过这些文件", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(rebackup, keep, skip);
                SkinManager.getInstance().applyTo(alert.getDialogPane());

                ButtonType result = alert.showAndWait().orElse(skip);
                if (result == rebackup) {
                    picked.set(ConflictChoice.REBACKUP);
                } else if (result == keep) {
                    picked.set(ConflictChoice.KEEP_BACKUP);
                } else {
                    picked.set(ConflictChoice.SKIP);
                }
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return picked.get();
    }

    private void restoreCurrent() {
        List<SkillFile> targets = getActionTargets();
        if (targets.isEmpty()) {
            UiHelper.warn("提示", "请先勾选文件或在左侧选择一个文件。");
            return;
        }

        List<SkillFile> restorable = new ArrayList<>();
        List<SkillFile> unknown = new ArrayList<>();
        for (SkillFile sf : targets) {
            if (sf.getStatus() != TranslateStatus.TRANSLATED) continue;
            if (state.getFileService().hasUnknownOriginal(sf.getFilePath())) {
                unknown.add(sf);
                continue;
            }
            restorable.add(sf);
        }
        if (restorable.isEmpty()) {
            String extra = unknown.isEmpty() ? ""
                    : "\n其中 " + unknown.size()
                            + " 个没有原始备份（曾被标记为已翻译），请先用「重新备份」登记原文。";
            UiHelper.warn("提示", "没有可还原的已翻译文件。" + extra);
            return;
        }
        String unknownNote = unknown.isEmpty() ? ""
                : "\n另有 " + unknown.size() + " 个没有原始备份，本次会跳过。";
        if (!UiHelper.confirm("确认",
                "确定要还原 " + restorable.size() + " 个文件吗？\n（"
                        + describeScope(restorable) + "）" + unknownNote)) {
            return;
        }

        int ok = 0;
        int fail = 0;
        for (SkillFile sf : restorable) {
            boolean success = state.getFileService().restoreFile(sf.getFilePath());
            if (success) {
                sf.setStatus(TranslateStatus.UNTRANSLATED);
                sf.setTranslatedDescription(null);
                ok++;
            } else {
                fail++;
            }
        }

        state.getRecordManager().flush();
        state.bumpVersion();
        showFile(state.getSelectedFile());
        if (fail == 0) {
            UiHelper.info("完成", "已还原 " + ok + " 个文件。");
        } else {
            UiHelper.warn("完成", "成功 " + ok + " 个，失败 " + fail + " 个。");
        }
    }

    private void rebackupCurrent() {
        List<SkillFile> targets = getActionTargets();
        if (targets.isEmpty()) {
            UiHelper.warn("提示", "请先勾选文件或在左侧选择一个文件。");
            return;
        }

        long translated = targets.stream()
                .filter(f -> f.getStatus() == TranslateStatus.TRANSLATED)
                .count();
        String message = "确定使用当前文件中的 Description 重新备份吗？\n"
                + describeScope(targets)
                + "，已存在的原始备份会被覆盖（旧备份留在记录里可查）。";
        if (translated > 0) {
            message = "有 " + translated + " 个文件已翻译，重新备份会把当前译文当作原始备份！\n\n"
                    + message;
        }
        if (!UiHelper.confirm("确认重新备份", message))
            return;

        int ok = 0;
        int fail = 0;
        for (SkillFile sf : targets) {
            boolean success = state.getFileService().rebackupFile(sf.getFilePath());
            if (success) {
                TranslationRecord rec = state.getRecordManager().get(sf.getFilePath());
                if (rec != null) {
                    sf.setOriginalDescription(rec.getOriginalDescription());
                }
                ok++;
            } else {
                fail++;
            }
        }

        state.getRecordManager().flush();
        state.bumpVersion();
        showFile(state.getSelectedFile());
        if (fail == 0) {
            UiHelper.info("完成", "已重新备份 " + ok + " 个文件。");
        } else {
            UiHelper.warn("完成", "成功 " + ok + " 个，失败 " + fail + " 个。");
        }
    }

    /**
     * 标记为已翻译：不改文件内容，只登记状态。
     * 原文优先从"内容相同的已翻译记录"里采用（同一技能分布在多个扫描目录时很有用）。
     */
    private void markTranslatedCurrent() {
        List<SkillFile> targets = getActionTargets();
        if (targets.isEmpty()) {
            UiHelper.warn("提示", "请先勾选文件或在左侧选择一个文件。");
            return;
        }

        int noBackup = 0;
        for (SkillFile sf : targets) {
            if (state.getFileService().hasUnknownOriginal(sf.getFilePath())) noBackup++;
        }
        StringBuilder msg = new StringBuilder();
        msg.append("把 ").append(describeScope(targets)).append(" 标记为已翻译？\n")
                .append("· 技能文件内容不会被修改，只登记状态\n")
                .append("· 原文备份优先从内容相同的已翻译记录里采用\n");
        if (noBackup > 0) {
            msg.append("· 其中 ").append(noBackup)
                    .append(" 个本来就没有原文备份，标记后依然没有\n");
        }
        msg.append("是否继续？");
        if (!UiHelper.confirm("标记为已翻译", msg.toString())) {
            return;
        }

        int ok = 0;
        int fail = 0;
        int adopted = 0;
        int unknown = 0;
        for (SkillFile sf : targets) {
            TranslationRecord rec = state.getFileService().markTranslatedFile(sf.getFilePath());
            if (rec == null) {
                fail++;
                continue;
            }
            ok++;
            if (rec.getOriginalSourcePath() != null) adopted++;
            if (rec.isOriginalUnknown()) unknown++;
            sf.setStatus(TranslateStatus.TRANSLATED);
            sf.setTranslatedDescription(rec.getTranslatedDescription());
            sf.setOriginalDescription(rec.getOriginalDescription());
        }

        state.getRecordManager().flush();
        state.bumpVersion();
        showFile(state.getSelectedFile());
        StringBuilder done = new StringBuilder("已标记 " + ok + " 个文件");
        if (adopted > 0) {
            done.append("，其中 ").append(adopted).append(" 个采用了其他记录的原文备份");
        }
        if (unknown > 0) {
            done.append("，另有 ").append(unknown).append(" 个没有原文备份（还原不可用）");
        }
        if (fail > 0) {
            done.append("，失败 ").append(fail).append(" 个（未读到 Description）");
        }
        UiHelper.info("标记完成", done.append("。").toString());
    }

    /**
     * 清空备份数据：作用于整个备份库（所有路径的记录），不再只看勾选/当前文件。
     *
     * <p>清空前先给 records.json 留一份带时间戳的副本，所以是可恢复的操作。</p>
     */
    private void clearBackupCurrent() {
        int records = state.getRecordManager().recordCount();
        int memory = state.getRecordManager().translationMemorySize();
        if (records == 0 && memory == 0) {
            UiHelper.info("清空备份数据", "备份库已经是空的。");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("清空备份数据");
        alert.setHeaderText(null);
        alert.setContentText("这是整库操作，作用于全部扫描目录：\n"
                + "· 备份记录 " + records + " 条（各文件的原始描述与译文状态）\n"
                + "· 翻译记忆 " + memory + " 条（原文→译文缓存）\n\n"
                + "技能文件本身不会被修改，但清空后所有文件都无法再「还原成备份」。\n"
                + "清空前会自动把 records.json 复制一份带时间戳的副本；保留翻译记忆可以省下重发请求的钱。");
        ButtonType wipeAll = new ButtonType("全部清空", ButtonBar.ButtonData.OK_DONE);
        ButtonType keepMemory = new ButtonType("只清备份记录", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(wipeAll, keepMemory, cancel);
        SkinManager.getInstance().applyTo(alert.getDialogPane());
        ButtonType picked = alert.showAndWait().orElse(cancel);
        if (picked != wipeAll && picked != keepMemory) {
            return;
        }
        boolean keepTranslationMemory = picked == keepMemory;

        Path recordsFile = state.getRecordManager().getRecordsFile();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneId.systemDefault()).format(Instant.now());
        boolean backedUp = ConfigManager.backupOnce(recordsFile, "clear-" + stamp);

        int cleared = state.getRecordManager().clearAll(keepTranslationMemory);
        for (SkillFile sf : state.getAllFiles()) {
            sf.setStatus(TranslateStatus.UNTRANSLATED);
            sf.setTranslatedDescription(null);
            sf.setOriginalDescription(null);
        }
        state.getRecordManager().flush();
        state.bumpVersion();
        state.refreshSelectedCount();
        showFile(state.getSelectedFile());
        log("清空备份库：记录 " + cleared + " 条，翻译记忆"
                + (keepTranslationMemory ? "保留" : "一并清空"));
        UiHelper.info("清空完成",
                "已清空 " + cleared + " 条备份记录"
                        + (keepTranslationMemory ? "，翻译记忆保留。" : "和全部翻译记忆。")
                        + (backedUp
                                ? "\n原文件已备份为 " + recordsFile.getFileName() + ".clear-" + stamp + ".bak"
                                : "\n（备份副本创建失败，请留意）"));
    }

    private void setProgressVisible(boolean visible) {
        progressBar.setVisible(visible);
        progressBar.setManaged(visible);
        progressLabel.setVisible(visible);
        progressLabel.setManaged(visible);
    }

    private void log(String msg) {
        LOG.info(msg);
    }
}
