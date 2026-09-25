package com.song.service;

import com.song.model.EffectType;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import com.song.util.ContentHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * 单个（或一批）SKILL.md 的备份、翻译与还原。
 *
 * <p>备份语义：备份 = "即将被覆盖掉的那段 description"，
 * 只在第一次翻译该文件时产生（不在扫描阶段产生）。</p>
 *
 * <p>批量翻译：每个文件仍然独立处理（备份、冲突、缓存、写回都按文件走）；
 * 只有"确实要发请求"这一步，若 provider 支持（大模型），才把几段原文拼成一次对话，
 * 再按标记拆回逐段结果；任何异常都回退成逐段翻译，段与段之间互不影响。</p>
 */
public class SkillFileService {

    private static final Logger LOG = LoggerFactory.getLogger(SkillFileService.class);

    /** "翻译+原文" 在多行内容时的分隔行。 */
    static final String KEEP_ENGLISH_DIVIDER = "/";

    private final RecordManager recordManager;
    private final TranslationService translationService;

    public SkillFileService(RecordManager recordManager, TranslationService translationService) {
        this.recordManager = recordManager;
        this.translationService = translationService;
    }

    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    public static void writeFile(String filePath, String content) throws IOException {
        Path target = Paths.get(filePath);
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 重新备份：强制使用当前文件中的 Description 覆盖已有备份。
     * 旧备份会留存在记录里（previousOriginalDescription），便于回滚。
     *
     * @param filePath 文件路径
     * @return 是否成功
     */
    public boolean rebackupFile(String filePath) {
        try {
            String content = readFile(filePath);
            String original = SkillParser.extractDescription(content);
            if (original == null) {
                LOG.warn("重新备份失败，未找到 Description: {}", filePath);
                return false;
            }
            recordManager.rebackup(filePath, original);
            recordManager.flush();
            LOG.info("重新备份成功: {}", filePath);
            return true;
        } catch (Exception e) {
            LOG.error("重新备份异常: {}", filePath, e);
            return false;
        }
    }

    /**
     * 文件当前内容是否与"我们上次写进去的"不一致（被外部改过、或备份还是旧版本内容）。
     *
     * @param filePath 文件路径
     * @return true 表示需要用户决定备份策略
     */
    public boolean hasConflict(String filePath) {
        try {
            return isConflict(filePath, SkillParser.extractDescription(readFile(filePath)));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isConflict(String filePath, String current) {
        if (current == null) {
            return false;
        }
        TranslationRecord rec = recordManager.get(filePath);
        if (rec == null) {
            return false;
        }
        String backup = rec.getOriginalDescription();
        if (backup == null || backup.isEmpty()) {
            return false;
        }
        String lastWritten = rec.getTranslatedDescription();
        String expected = (lastWritten != null && !lastWritten.isEmpty()) ? lastWritten : backup;
        // 优先按内容指纹比较（记录多、描述长时更省事），没有指纹再逐字比较
        String expectedHash = ContentHash.of(expected);
        String currentHash = ContentHash.of(current);
        if (!expectedHash.isEmpty() && !currentHash.isEmpty()) {
            return !expectedHash.equals(currentHash);
        }
        return !ContentHash.sameText(expected, current);
    }

    /**
     * 该文件是否"标记为已翻译但不知道原文"（此时还原没有意义）。
     *
     * @param filePath 文件路径
     * @return true 表示没有可用的原文备份
     */
    public boolean hasUnknownOriginal(String filePath) {
        TranslationRecord rec = recordManager.get(filePath);
        return rec != null && rec.isOriginalUnknown();
    }

    /**
     * 标记为已翻译：只登记状态，文件内容不动。原文优先从内容相同的记录里采用。
     *
     * @param filePath 文件路径
     * @return 采用的记录；读不到描述返回 null
     */
    public TranslationRecord markTranslatedFile(String filePath) {
        try {
            String current = SkillParser.extractDescription(readFile(filePath));
            if (current == null) {
                LOG.warn("标记为已翻译失败，未找到 Description: {}", filePath);
                return null;
            }
            TranslationRecord rec = recordManager.markTranslated(filePath, current);
            recordManager.flush();
            return rec;
        } catch (IOException e) {
            LOG.error("标记为已翻译异常: {}", filePath, e);
            return null;
        }
    }

    /**
     * 清空该文件的备份数据（技能文件不动，翻译记忆保留）。
     *
     * @param filePath 文件路径
     * @return 是否清掉了记录
     */
    public boolean clearBackup(String filePath) {
        String current = null;
        try {
            current = SkillParser.extractDescription(readFile(filePath));
        } catch (IOException e) {
            LOG.warn("清空备份前读取失败（不影响清空）: {}", filePath);
        }
        boolean cleared = recordManager.clearBackup(filePath, current);
        recordManager.flush();
        return cleared;
    }

    // ==================== 单文件翻译 ====================

    /**
     * 翻译一个文件并写回（命中翻译记忆时不再请求 API）。
     */
    public TranslationResult translateFile(String filePath, EffectType effect,
                                           String from, String to) {
        return translateFile(filePath, effect, from, to, true, false);
    }

    public TranslationResult translateFile(String filePath, EffectType effect,
                                           String from, String to, boolean useCache) {
        return translateFile(filePath, effect, from, to, useCache, false);
    }

    /**
     * 翻译一个文件并写回。
     *
     * @param filePath      文件路径
     * @param effect        翻译效果
     * @param from          源语言
     * @param to            目标语言
     * @param useCache      是否允许命中翻译记忆
     * @param refreshBackup true 表示先把当前内容重新登记为原始备份（用户选了"按当前文件重新备份"）
     * @return 结果；原文与备份不一致且未选择策略时返回 {@link TranslationResult#conflict(String)}
     */
    public TranslationResult translateFile(String filePath, EffectType effect,
                                           String from, String to, boolean useCache,
                                           boolean refreshBackup) {
        if (stopped()) {
            return TranslationResult.interrupted();
        }
        Prepared prepared = prepare(filePath, from, to, refreshBackup);
        if (prepared.early != null) {
            return prepared.early;
        }
        try {
            String translated = useCache
                    ? recordManager.getCachedTranslation(prepared.cacheKey, prepared.legacyKey)
                    : null;
            boolean fromCache = translated != null;
            if (!fromCache) {
                TranslationResult result = translateOne(prepared.sourceText, from, to);
                if (result.isInterrupted() || !result.isSuccess()) {
                    if (!result.isInterrupted()) {
                        recordManager.recordFailure(filePath);
                    }
                    return result;
                }
                translated = result.getText();
                recordManager.putCachedTranslation(prepared.cacheKey, translated);
                LOG.info("翻译缓存已更新: {}", filePath);
            }

            LOG.debug("翻译内容 [{}]{}原文 {} 字，译文 {} 字",
                    filePath, fromCache ? "（命中缓存）" : " ",
                    prepared.sourceText.length(), translated.length());

            TranslationResult written = writeBack(filePath, prepared, effect, translated);
            if (written.isSuccess()) {
                LOG.info(fromCache ? "翻译命中缓存: {}" : "翻译成功: {}", filePath);
            }
            return written;
        } catch (Exception e) {
            if (stopped()) {
                return TranslationResult.interrupted();
            }
            LOG.error("翻译异常: {}", filePath, e);
            recordManager.recordFailure(filePath);
            return TranslationResult.failure(e.getMessage() == null ? "翻译异常" : e.getMessage());
        }
    }

    // ==================== 批量翻译 ====================

    /**
     * 批量翻译多个文件：单文件仍独立处理，只有发请求那一步可能拼成一次大模型对话。
     *
     * @param filePaths      文件路径（结果顺序与之一致）
     * @param effect         翻译效果
     * @param from           源语言
     * @param to             目标语言
     * @param useCache       是否允许命中翻译记忆
     * @param refreshBackups 需要"按当前文件重新备份"的路径集合，可为 null
     * @param onFileDone     每完成一个文件回调一次（已完成数），可为 null
     * @return 与 filePaths 等长的结果；未处理完的（被中断）返回 {@link TranslationResult#interrupted()}
     */
    public List<TranslationResult> translateFiles(List<String> filePaths, EffectType effect,
                                                  String from, String to, boolean useCache,
                                                  Set<String> refreshBackups,
                                                  IntConsumer onFileDone) {
        int total = filePaths.size();
        TranslationResult[] results = new TranslationResult[total];
        Prepared[] prepared = new Prepared[total];
        List<Integer> pending = new ArrayList<>();
        int done = 0;

        // 1) 逐文件准备 + 吃缓存 + 单独写回
        for (int i = 0; i < total; i++) {
            String path = filePaths.get(i);
            if (stopped()) {
                break;
            }
            boolean refresh = refreshBackups != null && refreshBackups.contains(path);
            Prepared p = prepare(path, from, to, refresh);
            prepared[i] = p;
            if (p.early != null) {
                results[i] = p.early;
                done = report(onFileDone, done + 1);
                continue;
            }
            String cached = useCache
                    ? recordManager.getCachedTranslation(p.cacheKey, p.legacyKey) : null;
            if (cached != null) {
                results[i] = writeBack(path, p, effect, cached);
                done = report(onFileDone, done + 1);
            } else {
                pending.add(i);
            }
        }

        // 2) 待翻译项分组：每组不超过 maxBatchItems 段、maxBatchChars 字符
        int[] sizes = new int[total];
        for (int i = 0; i < total; i++) {
            sizes[i] = prepared[i] == null || prepared[i].sourceText == null
                    ? 0 : prepared[i].sourceText.length();
        }
        for (List<Integer> group : planGroups(pending,
                sizes, translationService.maxBatchItems(), translationService.maxBatchChars())) {
            if (stopped()) {
                break;
            }
            List<TranslationResult> groupResults = group.size() > 1
                    ? translateGroup(filePaths, prepared, group, effect, from, to)
                    : List.of(translatePending(filePaths.get(group.get(0)),
                            prepared[group.get(0)], effect, from, to));
            for (int k = 0; k < group.size(); k++) {
                results[group.get(k)] = groupResults.get(k);
            }
            for (int ignored : group) {
                done = report(onFileDone, done + 1);
            }
        }

        // 3) 未处理的（中断）按中断返回
        List<TranslationResult> out = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            out.add(results[i] != null ? results[i] : TranslationResult.interrupted());
        }
        return out;
    }

    /**
     * 把待翻译项分组：每组不超过 maxItems 段、不超过 maxChars 字符。
     * 单项就超过 maxChars 时独占一组（不丢弃）。
     *
     * @param pending  待翻译项下标
     * @param sizes    每项原文长度
     * @param maxItems 每组最大段数；&lt;=0 表示不限制
     * @param maxChars 每组最大字符数；&lt;=0 表示不限制
     * @return 分组
     */
    static List<List<Integer>> planGroups(List<Integer> pending, int[] sizes,
                                          int maxItems, int maxChars) {
        List<List<Integer>> groups = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int chars = 0;
        for (int index : pending) {
            int size = index < sizes.length ? sizes[index] : 0;
            boolean full = !current.isEmpty()
                    && ((maxItems > 0 && current.size() + 1 > maxItems)
                    || (maxChars > 0 && chars + size > maxChars));
            if (full) {
                groups.add(current);
                current = new ArrayList<>();
                chars = 0;
            }
            current.add(index);
            chars += size;
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /** 一次批量请求：拼成一次对话，拆回后逐文件写回；失败回退逐段。 */
    private List<TranslationResult> translateGroup(List<String> filePaths, Prepared[] prepared,
                                                   List<Integer> group, EffectType effect,
                                                   String from, String to) {
        List<String> texts = new ArrayList<>(group.size());
        for (int index : group) {
            texts.add(prepared[index].sourceText);
        }
        List<String> translated;
        try {
            translated = translationService.translateBatch(texts, from, to);
            if (translated == null || translated.size() != group.size()) {
                throw new IllegalStateException("批量翻译返回段数不符");
            }
        } catch (Exception e) {
            if (HttpCalls.causedByCancel(e) || stopped()) {
                List<TranslationResult> interrupted = new ArrayList<>(group.size());
                for (int i = 0; i < group.size(); i++) {
                    interrupted.add(TranslationResult.interrupted());
                }
                return interrupted;
            }
            // 一段出问题不能影响其他段：回退成逐段
            LOG.warn("批量翻译失败，回退逐段（{} 段）：{}", group.size(), e.getMessage());
            List<TranslationResult> fallback = new ArrayList<>(group.size());
            for (int index : group) {
                fallback.add(translatePending(filePaths.get(index), prepared[index],
                        effect, from, to));
            }
            return fallback;
        }

        List<TranslationResult> out = new ArrayList<>(group.size());
        for (int k = 0; k < group.size(); k++) {
            int index = group.get(k);
            String text = translated.get(k);
            recordManager.putCachedTranslation(prepared[index].cacheKey, text);
            LOG.info("批量翻译成功: {}（本次 {} 段）", filePaths.get(index), group.size());
            out.add(writeBack(filePaths.get(index), prepared[index], effect, text));
        }
        return out;
    }

    /** 单个待翻译项：请求 → 记缓存 → 写回。 */
    private TranslationResult translatePending(String filePath, Prepared prepared,
                                               EffectType effect, String from, String to) {
        TranslationResult result = translateOne(prepared.sourceText, from, to);
        if (result.isInterrupted() || !result.isSuccess()) {
            if (!result.isInterrupted()) {
                recordManager.recordFailure(filePath);
            }
            return result;
        }
        recordManager.putCachedTranslation(prepared.cacheKey, result.getText());
        if (!stopped()) {
            LOG.info("翻译成功: {}", filePath);
        }
        return writeBack(filePath, prepared, effect, result.getText());
    }

    /** 单文件的准备结果。 */
    private static final class Prepared {
        final String content;
        final String sourceText;
        final String cacheKey;
        final String legacyKey;
        /** 非 null 表示不用翻译（冲突/读不到 Description 等）。 */
        final TranslationResult early;

        Prepared(String content, String sourceText, String cacheKey, String legacyKey) {
            this.content = content;
            this.sourceText = sourceText;
            this.cacheKey = cacheKey;
            this.legacyKey = legacyKey;
            this.early = null;
        }

        Prepared(TranslationResult early) {
            this.content = null;
            this.sourceText = null;
            this.cacheKey = null;
            this.legacyKey = null;
            this.early = early;
        }
    }

    /** 读文件、判冲突、按需建首次备份，算出缓存键。 */
    private Prepared prepare(String filePath, String from, String to, boolean refreshBackup) {
        try {
            String content = readFile(filePath);
            String current = SkillParser.extractDescription(content);
            if (current == null) {
                LOG.warn("翻译失败，未找到 Description 字段: {}", filePath);
                recordManager.recordFailure(filePath);
                return new Prepared(TranslationResult.failure("未找到 Description 字段"));
            }
            if (refreshBackup) {
                recordManager.rebackup(filePath, current);
                LOG.info("按当前文件内容重新备份: {}", filePath);
            } else if (isConflict(filePath, current)) {
                LOG.warn("原文与备份不一致: {}", filePath);
                return new Prepared(TranslationResult.conflict(current));
            }

            String sourceText = recordManager.getOriginalDescription(filePath);
            if (sourceText == null || sourceText.isEmpty()) {
                recordManager.recordFirstBackup(filePath, current);
                sourceText = recordManager.getOriginalDescription(filePath);
            }
            if (sourceText == null || sourceText.isEmpty()) {
                sourceText = current;
            }
            return new Prepared(content, sourceText,
                    buildCacheKey(sourceText, from, to), buildLegacyCacheKey(sourceText, from, to));
        } catch (IOException e) {
            LOG.error("读取失败: {}", filePath, e);
            recordManager.recordFailure(filePath);
            return new Prepared(TranslationResult.failure(
                    e.getMessage() == null ? "读取失败" : e.getMessage()));
        }
    }

    /** 逐段请求（超过 provider 的分段上限时先切段再拼回）。 */
    private TranslationResult translateOne(String sourceText, String from, String to) {
        int limit = translationService.maxChunkLength();
        List<String> chunks = sourceText.length() <= limit
                ? List.of(sourceText)
                : TextSegmenter.split(sourceText, limit);

        StringBuilder full = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                if (full.length() > 0) full.append('\n');
                continue;
            }
            if (stopped()) {
                return TranslationResult.interrupted();
            }
            TranslationResult result = translationService.translate(chunk, from, to);
            if (result.isInterrupted() || !result.isSuccess()) {
                return result;
            }
            if (full.length() > 0) full.append('\n');
            full.append(result.getText());
        }
        return TranslationResult.success(full.toString());
    }

    /** 写回文件 + 记记录。 */
    private TranslationResult writeBack(String filePath, Prepared prepared, EffectType effect,
                                        String translated) {
        if (stopped()) {
            return TranslationResult.interrupted();
        }
        try {
            String newDesc = (effect == EffectType.OVERWRITE)
                    ? translated
                    : composeKeepEnglish(translated, prepared.sourceText);
            String newContent = SkillParser.replaceDescription(prepared.content, newDesc);
            writeFile(filePath, newContent);
            recordManager.recordTranslation(filePath, newDesc, effect, TranslateStatus.TRANSLATED);
            return TranslationResult.success(newDesc);
        } catch (IOException e) {
            LOG.error("写回失败: {}", filePath, e);
            recordManager.recordFailure(filePath);
            return TranslationResult.failure(e.getMessage() == null ? "写回失败" : e.getMessage());
        }
    }

    private static int report(IntConsumer onFileDone, int done) {
        if (onFileDone != null) {
            onFileDone.accept(done);
        }
        return done;
    }

    /**
     * 拼"翻译+原文"。多行时把分隔符单独放一行，避免埋在 YAML 块标量中间看不清。
     *
     * @param translated 译文
     * @param sourceText 原文
     * @return 写回内容
     */
    static String composeKeepEnglish(String translated, String sourceText) {
        if (translated != null && translated.indexOf('\n') >= 0) {
            return translated + "\n\n" + KEEP_ENGLISH_DIVIDER + "\n" + sourceText;
        }
        return translated + KEEP_ENGLISH_DIVIDER + sourceText;
    }

    private static boolean stopped() {
        return HttpCalls.isCancelled();
    }

    /**
     * 新键：厂商 + 模型 + 提示词指纹 + 语种 + 原文。
     * 换了模型或改了提示词就不会再吃到旧译文。
     */
    private String buildCacheKey(String original, String from, String to) {
        var provider = translationService.getProvider();
        String providerId = provider != null ? provider.getType().getId() : "unknown";
        String model = translationService.modelTag();
        String prompt = translationService.promptFingerprint();
        return "v2|" + providerId + "|" + model + "|" + prompt + "|" + from + "|" + to + "|" + original;
    }

    /** 旧键（不含模型与提示词），用于读老版本写下的翻译记忆。 */
    private String buildLegacyCacheKey(String original, String from, String to) {
        var provider = translationService.getProvider();
        String providerId = provider != null ? provider.getType().getId() : "unknown";
        return providerId + "|" + from + "|" + to + "|" + original;
    }

    /** 还原最初：把 originalDescription 写回文件 */
    public boolean restoreFile(String filePath) {
        try {
            TranslationRecord rec = recordManager.get(filePath);
            if (rec != null && rec.isOriginalUnknown()) {
                LOG.warn("还原失败，没有原始备份（曾被标记为已翻译）: {}", filePath);
                return false;
            }
            String original = recordManager.getOriginalDescription(filePath);
            if (original == null) {
                LOG.warn("还原失败，无原始备份: {}", filePath);
                return false;
            }
            String content = readFile(filePath);
            String newContent = SkillParser.replaceDescription(content, original);
            writeFile(filePath, newContent);

            recordManager.recordTranslation(filePath, null, null, TranslateStatus.UNTRANSLATED);
            recordManager.flush();
            LOG.info("还原成功: {}", filePath);
            return true;
        } catch (Exception e) {
            LOG.error("还原异常: {}", filePath, e);
            return false;
        }
    }
}
