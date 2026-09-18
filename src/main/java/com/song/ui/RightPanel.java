package com.song.ui;

import com.song.model.*;
import com.song.model.EffectType;
import com.song.model.SkillFile;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import com.song.service.SkillFileService;
import com.song.service.SkillParser;
import com.song.service.TranslationResult;
import com.song.skin.control.AnimatedButton;
import com.song.skin.control.AnimatedTextArea;
import com.song.util.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class RightPanel extends VBox {

    private static final Logger LOG = LoggerFactory.getLogger(RightPanel.class);

    private final AppState state;

    private final Label originalLabel = new Label("备份数据：");
    private final AnimatedTextArea originalArea = new AnimatedTextArea();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Label translatedLabel = new Label("实际读取数据：");
//    private final Label backupTimeLabel = new Label("翻译更新时间：--");
    private final AnimatedTextArea translatedArea = new AnimatedTextArea();

    private final ToggleGroup effectGroup = new ToggleGroup();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("");
    private final AnimatedButton stopButton = new AnimatedButton("中断翻译");

    private volatile boolean stopFlag = false;
    private Thread worker;

    public RightPanel(AppState state) {
        this.state = state;
        setSpacing(0);
        setPadding(new Insets(8));
        getStyleClass().add("right-panel");



        originalLabel.setStyle("-fx-font-weight: bold;");
        originalArea.setEditable(false);
        originalArea.setWrapText(true);
        originalArea.setPrefRowCount(5);

//        backupTimeLabel.getStyleClass().add("secondary");
//        backupTimeLabel.setStyle("-fx-font-size: 11;");

        translatedLabel.setStyle("-fx-font-weight: bold;");
        translatedLabel.setVisible(true);
        translatedArea.setEditable(false);
        translatedArea.setWrapText(true);
        translatedArea.setPrefRowCount(5);
        translatedArea.setVisible(true);

        Label effectLabel = new Label("翻译效果：");
        HBox effectBox = new HBox(12);
        for (EffectType et : EffectType.values()) {
            RadioButton rb = new RadioButton(et.getLabel());
            rb.setToggleGroup(effectGroup);
            rb.setUserData(et);
            if (et == state.getEffect()) rb.setSelected(true);
            rb.selectedProperty().addListener((obs, o, n) -> {
                if (n) state.effectProperty().set(et);
            });
            effectBox.getChildren().add(rb);
        }

        AnimatedButton retranslate = new AnimatedButton("重新翻译");
        AnimatedButton transCur = new AnimatedButton("翻译当前文件");
        AnimatedButton transSel = new AnimatedButton("翻译勾选文件");
        AnimatedButton restore = new AnimatedButton("还原成备份");
        AnimatedButton rebackup = new AnimatedButton("重新备份");
        stopButton.setDisable(true);

        retranslate.setOnAction(e -> retranslateCurrent());
        transCur.setOnAction(e -> translateCurrent());
        transSel.setOnAction(e -> translateSelected());
        restore.setOnAction(e -> restoreCurrent());
        rebackup.setOnAction(e -> rebackupCurrent());
        stopButton.setOnAction(e -> stopFlag = true);

        for (Button b : List.of(retranslate, transCur, transSel, restore, rebackup, stopButton)) {
            b.setMinWidth(110);
            b.setPrefWidth(120);
        }

        HBox btnBox = new HBox(8, retranslate, transCur, transSel, stopButton, restore, rebackup);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        progressBar.setPrefWidth(220);
        HBox progBox = new HBox(8, progressBar, progressLabel);
        progBox.setAlignment(Pos.CENTER_LEFT);
        setProgressVisible(false);

        VBox topBox = new VBox(6,
                originalLabel, originalArea,
                translatedLabel, translatedArea);
        topBox.setPadding(new Insets(0, 0, 6, 0));

        VBox bottomBox = new VBox(6,
                effectLabel, effectBox,
                btnBox, progBox);
        bottomBox.setPadding(new Insets(6, 0, 0, 0));

        VBox.setVgrow(originalArea, Priority.ALWAYS);
        VBox.setVgrow(translatedArea, Priority.ALWAYS);

        SplitPane split = new SplitPane(topBox, bottomBox);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.65);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().add(split);

        state.selectedFileProperty().addListener((obs, o, n) -> showFile(n));
        state.versionProperty().addListener((obs, o, n) -> showFile(state.getSelectedFile()));
    }
    private void showFile(SkillFile sf) {
        if (sf == null) {
            originalLabel.setText("备份数据：");
            originalArea.clear();
//            backupTimeLabel.setText("翻译更新时间：--");
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

//        long lastTranslate = rec != null ? rec.getLastTranslateTimestamp() : 0L;
//        if (lastTranslate > 0) {
//            backupTimeLabel.setText("翻译更新时间："
//                    + TIME_FORMATTER.format(Instant.ofEpochMilli(lastTranslate)));
//        } else {
//            backupTimeLabel.setText("翻译更新时间：--");
//        }

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
            if (sf.isSelected()) checked.add(sf);
        }
        return checked;
    }

    private List<SkillFile> getActionTargets() {
        List<SkillFile> checked = getCheckedFiles();
        if (!checked.isEmpty()) return checked;

        SkillFile current = state.getSelectedFile();
        if (current == null) return List.of();
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
                        + "共 " + targets.size() + " 个文件，是否继续？")) {
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
        if (sf.getStatus() == TranslateStatus.TRANSLATED) {
            if (!UiHelper.confirm("确认", "该文件已经翻译过，继续会浪费 API 额度。是否继续？")) return;
        }
        List<SkillFile> targets = new ArrayList<>();
        targets.add(sf);
        startTranslate(targets, true);
    }

    private void translateSelected() {
        List<SkillFile> selected = getCheckedFiles();
        if (selected.isEmpty()) {
            UiHelper.warn("提示", "没有勾选任何文件。");
            return;
        }
        long already = selected.stream()
                .filter(f -> f.getStatus() == TranslateStatus.TRANSLATED).count();
        if (already > 0) {
            if (!UiHelper.confirm("确认",
                    "勾选的文件中有 " + already + " 个已经翻译过，继续会浪费 API 额度。是否继续？")) {
                return;
            }
        }
        startTranslate(selected, true);
    }

    /**
     * 根据 CPU、内存和任务数量动态计算线程数。
     * 任务很少时不会创建多余线程，机器配置较低时自动降低并发。
     */
    private int calculateThreadCount(int taskCount) {
        if (taskCount <= 0) return 0;

        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        int cpuBased = Math.max(1, cores);
        int memoryBased;
        if (maxMemoryMb < 512) {
            memoryBased = 2;
        } else if (maxMemoryMb < 1024) {
            memoryBased = 4;
        } else {
            memoryBased = 8;
        }

        int machineLimit = Math.max(1, Math.min(8, Math.min(cpuBased, memoryBased)));
        return Math.min(taskCount, machineLimit);
    }

    private void startTranslate(List<SkillFile> targets, boolean useCache) {
        if (worker != null && worker.isAlive()) {
            UiHelper.warn("提示", "已有翻译任务正在运行。");
            return;
        }
        stopFlag = false;
        stopButton.setDisable(false);
        EffectType effect = state.getEffect();
        String from = state.getConfig().getSourceLang();
        String to = state.getConfig().getTargetLang();
        int interval = state.getConfig().getRequestIntervalMs();
        int total = targets.size();
        int threadCount = calculateThreadCount(total);

        progressBar.setProgress(0);
        progressLabel.setText("0/" + total);
        setProgressVisible(true);
        log("开始并发翻译 " + total + " 个文件，线程数：" + threadCount
                + "，效果：" + effect.getLabel());

        worker = new Thread(() -> {
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            AtomicInteger ok = new AtomicInteger();
            AtomicInteger fail = new AtomicInteger();
            AtomicInteger completed = new AtomicInteger();
            Queue<String> errors = new ConcurrentLinkedQueue<>();
            CountDownLatch latch = new CountDownLatch(total);

            for (int i = 0; i < total; i++) {
                SkillFile sf = targets.get(i);
                int idx = i + 1;
                pool.submit(() -> {
                    try {
                        if (stopFlag) return;
                        log(String.format("[%d/%d] %s", idx, total, sf.getParentName()));


                        if (stopFlag) return;

                        TranslationResult result = state.getFileService()
                                .translateFile(sf.getFilePath(), effect, from, to, useCache);

                        if (result.isSuccess()) {
                            ok.incrementAndGet();
                            sf.setStatus(TranslateStatus.TRANSLATED);
                            TranslationRecord rec = state.getRecordManager().get(sf.getFilePath());
                            if (rec != null) sf.setTranslatedDescription(rec.getTranslatedDescription());
                            log("    OK");
                        } else {
                            fail.incrementAndGet();
                            sf.setStatus(TranslateStatus.FAILED);
                            String error = result.getErrorMessage() == null
                                    ? "翻译失败" : result.getErrorMessage();
                            errors.add(sf.getParentName() + "：" + error);
                            log("    失败：" + error);
                        }
                    } catch (Exception e) {
                        fail.incrementAndGet();
                        errors.add(sf.getParentName() + "：" + e.getMessage());
                    } finally {
                        int done = completed.incrementAndGet();
                        Platform.runLater(() -> {
                            progressBar.setProgress((double) done / total);
                            progressLabel.setText(done + "/" + total);
                            state.bumpVersion();
                        });
                        latch.countDown();
                    }
                });
            }

            pool.shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final int fok = ok.get();
            final int ffail = fail.get();
            Platform.runLater(() -> {
                log(String.format("完成：成功 %d，失败 %d", fok, ffail));
                stopButton.setDisable(true);
                progressLabel.setText("完成");
                setProgressVisible(false);
                state.bumpVersion();
                showFile(state.getSelectedFile());
                UiHelper.info("翻译完成", "成功 " + fok + "，失败 " + ffail);
                if (!errors.isEmpty()) {
                    UiHelper.error("翻译失败", String.join("\n", errors));
                }
            });
        }, "translate-manager");
        worker.setDaemon(true);
        worker.start();
    }
    private void restoreCurrent() {
        List<SkillFile> targets = getActionTargets();
        if (targets.isEmpty()) {
            UiHelper.warn("提示", "请先勾选文件或在左侧选择一个文件。");
            return;
        }

        List<SkillFile> restorable = new ArrayList<>();
        for (SkillFile sf : targets) {
            if (sf.getStatus() == TranslateStatus.TRANSLATED) restorable.add(sf);
        }
        if (restorable.isEmpty()) {
            UiHelper.warn("提示", "没有可还原的已翻译文件。");
            return;
        }
        if (!UiHelper.confirm("确认", "确定要还原 " + restorable.size() + " 个文件吗？")) return;

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
                + "共 " + targets.size() + " 个文件，已存在的原始备份将被覆盖。";
        if (translated > 0) {
            message = "有 " + translated + " 个文件已翻译，重新备份会把当前译文当作原始备份！\n\n"
                    + message;
        }
        if (!UiHelper.confirm("确认重新备份", message)) return;

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

        state.bumpVersion();
        showFile(state.getSelectedFile());
        if (fail == 0) {
            UiHelper.info("完成", "已重新备份 " + ok + " 个文件。");
        } else {
            UiHelper.warn("完成", "成功 " + ok + " 个，失败 " + fail + " 个。");
        }
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