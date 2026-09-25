package com.song.service;

import com.song.config.ConfigManager;
import com.song.model.*;
import com.song.util.ContentHash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.song.model.EffectType;
import com.song.model.RecordsWrapper;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 备份记录与翻译记忆（{@code ~/.easyReadSkill/records.json}）。
 *
 * <p>两条账分开记：</p>
 * <ul>
 *   <li><b>按路径记账</b>：每条记录属于一个文件路径，状态判断以这个为准。</li>
 *   <li><b>按内容指纹记账</b>（{@link ContentHash}）：同一段 description 出现在多个目录、
 *       技能被改名或复制时，用它把"原文备份"接过来，不把译文当成原文。</li>
 * </ul>
 *
 * <p>写入策略：改动只置脏标记，由调用方在批处理结束时 {@link #flush()}。</p>
 */
public class RecordManager {

    private static final Logger LOG = LoggerFactory.getLogger(RecordManager.class);

    private static final String RECORDS_NAME = "records.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path recordsFile;
    private final RecordsWrapper wrapper;
    private boolean dirty;

    /** 译文指纹 → 记录，懒构建。 */
    private Map<String, List<TranslationRecord>> translationIndex;
    /** 规范化路径 → 记录键，懒构建（同一物理文件从不同扫描根命中时用）。 */
    private Map<String, String> canonicalPathIndex;

    public RecordManager() {
        this(ConfigManager.getDataDir());
    }

    /**
     * @param dataDir 数据目录（测试或自定义位置）
     */
    public RecordManager(Path dataDir) {
        Path dir = dataDir == null ? ConfigManager.getDataDir() : dataDir;
        this.recordsFile = dir.resolve(RECORDS_NAME);
        this.wrapper = loadFromDisk();
    }

    /** @return records.json 路径 */
    public Path getRecordsFile() {
        return recordsFile;
    }

    private RecordsWrapper loadFromDisk() {
        try {
            if (!Files.exists(recordsFile)) return new RecordsWrapper();
            String json = Files.readString(recordsFile, StandardCharsets.UTF_8);
            RecordsWrapper w = GSON.fromJson(json, RecordsWrapper.class);
            if (w == null) return new RecordsWrapper();
            if (w.getRecords() == null) w.setRecords(new HashMap<>());
            if (w.getTranslationMemory() == null) w.setTranslationMemory(new HashMap<>());
            if (w.getSuppressedHashes() == null) w.setSuppressedHashes(new HashMap<>());
            return w;
        } catch (IOException e) {
            LOG.error("读取翻译记录失败: {}", recordsFile, e);
            return new RecordsWrapper();
        } catch (RuntimeException e) {
            // JSON 损坏时留一份现场，避免每次启动都崩在反序列化上
            LOG.error("翻译记录损坏: {}", recordsFile, e);
            quarantine();
            return new RecordsWrapper();
        }
    }

    private void quarantine() {
        try {
            Path bad = recordsFile.resolveSibling(RECORDS_NAME + ".bad");
            Files.move(recordsFile, bad, StandardCopyOption.REPLACE_EXISTING);
            LOG.warn("已把损坏的记录文件移到 {}", bad);
        } catch (IOException e) {
            LOG.warn("备份损坏记录失败: {}", e.toString());
        }
    }

    /** 标记有改动，等待 {@link #flush()}。 */
    public synchronized void markDirty() {
        dirty = true;
    }

    /** 有待写入的改动则落盘。 */
    public synchronized void flush() {
        if (!dirty) return;
        saveToDisk();
    }

    /** 全量写盘（原子替换）。 */
    public synchronized void saveToDisk() {
        try {
            Path dir = recordsFile.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            String json = GSON.toJson(wrapper);
            Path tmp = recordsFile.resolveSibling(RECORDS_NAME + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, recordsFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            LOG.error("保存翻译记录失败: {}", recordsFile, e);
        }
    }

    // ==================== 翻译记忆 ====================

    public synchronized String getCachedTranslation(String key) {
        if (key == null) return null;
        return wrapper.getTranslationMemory().get(key);
    }

    /**
     * 查翻译记忆：先查新键，未命中再回退旧键（老键不含模型与提示词）。
     * 旧键命中后顺手写入新键，完成渐进升级。
     *
     * @param key       新键
     * @param legacyKey 旧键，可为 null
     * @return 译文或 null
     */
    public synchronized String getCachedTranslation(String key, String legacyKey) {
        if (key == null) {
            return legacyKey == null ? null : wrapper.getTranslationMemory().get(legacyKey);
        }
        String hit = wrapper.getTranslationMemory().get(key);
        if (hit != null) return hit;
        if (legacyKey == null || legacyKey.equals(key)) return null;
        String legacy = wrapper.getTranslationMemory().get(legacyKey);
        if (legacy != null) {
            wrapper.getTranslationMemory().put(key, legacy);
            markDirty();
        }
        return legacy;
    }

    public synchronized void putCachedTranslation(String key, String translatedText) {
        if (key == null || translatedText == null) return;
        wrapper.getTranslationMemory().put(key, translatedText);
        markDirty();
    }

    // ==================== 记录读取 ====================

    /**
     * 取记录：先按路径精确匹配；没有时按"同一物理文件"匹配（同一技能被两个扫描根命中、
     * 或用别名路径访问），命中后把记录改挂到当前路径。
     *
     * @param path 文件路径
     * @return 记录或 null
     */
    public synchronized TranslationRecord get(String path) {
        if (path == null) return null;
        TranslationRecord rec = wrapper.getRecords().get(path);
        if (rec != null) return rec;
        return rekeyAlias(path);
    }

    private TranslationRecord rekeyAlias(String path) {
        String canonical = canonicalOf(path);
        if (canonical == null) return null;
        String existingKey = canonicalPathIndex().get(canonical);
        if (existingKey == null || existingKey.equals(path)) return null;
        TranslationRecord rec = wrapper.getRecords().remove(existingKey);
        if (rec == null) return null;
        rec.setPath(path);
        wrapper.getRecords().put(path, rec);
        invalidateIndexes();
        markDirty();
        LOG.info("同一文件换了路径，记录跟随: {} <- {}", path, existingKey);
        return rec;
    }

    public synchronized String getOriginalDescription(String path) {
        TranslationRecord rec = get(path);
        return rec != null ? rec.getOriginalDescription() : null;
    }

    /** @return 记录快照（便于跨线程遍历） */
    public synchronized Map<String, TranslationRecord> all() {
        return new HashMap<>(wrapper.getRecords());
    }

    /**
     * 内容是否等于某条已翻译记录的译文（同一技能分布在多个目录）。
     *
     * @param description 描述文本
     * @return 是否有匹配
     */
    public synchronized boolean matchesKnownTranslation(String description) {
        return findTranslationDonor(description, null) != null;
    }

    /**
     * 文件没有记录、但当前内容正是别处记录翻出来的译文时，把它按"已翻译"登记，
     * 并采用那条记录的原文备份（同一技能分布在多个扫描目录的常见情况）。
     *
     * @param path 文件路径
     * @param currentDescription 文件当前描述
     * @return 采用的记录；没有可用来源返回 null
     */
    public synchronized TranslationRecord adoptKnownTranslation(String path,
                                                                String currentDescription) {
        if (isSuppressed(path, currentDescription)) {
            return null;
        }
        TranslationRecord donor = findTranslationDonor(currentDescription, path);
        if (donor == null) {
            return null;
        }
        TranslationRecord rec = new TranslationRecord(path, donor.getOriginalDescription());
        rec.setOriginalSourcePath(donor.getPath());
        rec.setStatus(TranslateStatus.TRANSLATED);
        rec.setTranslatedDescription(currentDescription);
        rec.setLastTranslateTimestamp(System.currentTimeMillis());
        stamp(rec);
        wrapper.getRecords().put(path, rec);
        invalidateIndexes();
        markDirty();
        LOG.info("内容与已翻译记录一致，采用其原文备份: {} <- {}", path, donor.getPath());
        return rec;
    }

    // ==================== 记录写入 ====================

    /**
     * 首次备份。若文件当前内容其实是另一条记录的译文（技能目录被移动、改名或复制），
     * 则继承那份记录的原始备份，避免把译文登记成"原文"。
     *
     * @param path 文件路径
     * @param currentDescription 文件当前描述
     * @return 记录
     */
    public synchronized TranslationRecord recordFirstBackup(String path, String currentDescription) {
        Map<String, TranslationRecord> map = wrapper.getRecords();
        TranslationRecord rec = map.get(path);
        if (rec != null && hasOriginal(rec)) {
            return rec;
        }
        String original = currentDescription;
        TranslationRecord donor = findTranslationDonor(currentDescription, path);
        if (donor != null) {
            LOG.info("原文备份从内容相同的记录继承: {} <- {}", path, donor.getPath());
            original = donor.getOriginalDescription();
        }
        if (rec == null) {
            rec = new TranslationRecord(path, original);
            map.put(path, rec);
        } else {
            rec.setOriginalDescription(original);
            rec.setFirstBackupTimestamp(System.currentTimeMillis());
        }
        rec.setOriginalSourcePath(donor != null ? donor.getPath() : null);
        rec.setOriginalUnknown(false);
        stamp(rec);
        wrapper.getSuppressedHashes().remove(path);
        invalidateIndexes();
        markDirty();
        return rec;
    }

    /**
     * 强制重新备份：覆盖已有 originalDescription，并把旧备份留一份用于回滚。
     *
     * @param path 文件路径
     * @param originalDescription 新的原始备份
     * @return 记录
     */
    public synchronized TranslationRecord rebackup(String path, String originalDescription) {
        Map<String, TranslationRecord> map = wrapper.getRecords();
        TranslationRecord rec = map.get(path);
        if (rec == null) {
            rec = new TranslationRecord(path, originalDescription);
            map.put(path, rec);
        } else {
            if (rec.getOriginalDescription() != null && !rec.getOriginalDescription().isEmpty()) {
                rec.setPreviousOriginalDescription(rec.getOriginalDescription());
                rec.setPreviousOriginalTimestamp(rec.getFirstBackupTimestamp());
            }
            rec.setOriginalDescription(originalDescription);
            rec.setFirstBackupTimestamp(System.currentTimeMillis());
        }
        rec.setOriginalUnknown(false);
        rec.setOriginalSourcePath(null);
        stamp(rec);
        wrapper.getSuppressedHashes().remove(path);
        invalidateIndexes();
        markDirty();
        return rec;
    }

    /**
     * 用户手动"标记为已翻译"：文件内容保持不变，只把状态登记为已翻译。
     *
     * <p>原文备份优先从内容相同的已翻译记录里采用；找不到就记成"原文未知"，
     * 这样"还原成备份"不会假装成功，而是明确告诉用户没有原文。</p>
     *
     * @param path 文件路径
     * @param currentDescription 文件当前描述（就是译文）
     * @return 记录
     */
    public synchronized TranslationRecord markTranslated(String path, String currentDescription) {
        Map<String, TranslationRecord> map = wrapper.getRecords();
        TranslationRecord rec = map.get(path);
        boolean fresh = rec == null;
        if (rec == null) {
            rec = new TranslationRecord(path, currentDescription);
            map.put(path, rec);
        }
        // 刚建出来的记录，original 只是占位（等于当前内容），不能当"已有原文备份"
        boolean hasBackup = !fresh && hasOriginal(rec) && !rec.isOriginalUnknown();
        rec.setStatus(TranslateStatus.TRANSLATED);
        rec.setTranslatedDescription(currentDescription);
        rec.setLastTranslateTimestamp(System.currentTimeMillis());
        if (!hasBackup) {
            TranslationRecord donor = findTranslationDonor(currentDescription, path);
            if (donor != null) {
                rec.setOriginalDescription(donor.getOriginalDescription());
                rec.setOriginalSourcePath(donor.getPath());
                rec.setOriginalUnknown(false);
                LOG.info("标记为已翻译并采用内容相同的原文备份: {} <- {}", path, donor.getPath());
            } else {
                rec.setOriginalDescription(currentDescription);
                rec.setOriginalUnknown(true);
                LOG.info("标记为已翻译，但没有可采用的原文备份: {}", path);
            }
        }
        stamp(rec);
        wrapper.getSuppressedHashes().remove(path);
        invalidateIndexes();
        markDirty();
        return rec;
    }

    /**
     * 清空某个文件的备份数据，并压住"自动采用原文"（内容不变就不再自动采用）。
     *
     * @param path 文件路径
     * @param currentDescription 文件当前描述，用于记录压制指纹
     * @return 是否删掉了记录
     */
    public synchronized boolean clearBackup(String path, String currentDescription) {
        if (path == null) return false;
        TranslationRecord removed = wrapper.getRecords().remove(path);
        String hash = ContentHash.of(currentDescription);
        if (hash.isEmpty()) {
            wrapper.getSuppressedHashes().remove(path);
        } else {
            wrapper.getSuppressedHashes().put(path, hash);
        }
        invalidateIndexes();
        markDirty();
        return removed != null;
    }

    /**
     * 清空整个备份库（所有路径的记录一起删）。
     *
     * @param keepTranslationMemory true 表示保留翻译记忆缓存（下次翻译还能省钱）
     * @return 清掉的记录条数
     */
    public synchronized int clearAll(boolean keepTranslationMemory) {
        int count = wrapper.getRecords().size();
        wrapper.getRecords().clear();
        wrapper.getSuppressedHashes().clear();
        if (!keepTranslationMemory) {
            wrapper.getTranslationMemory().clear();
        }
        invalidateIndexes();
        saveToDisk();
        return count;
    }

    /** @return 翻译记忆条数 */
    public synchronized int translationMemorySize() {
        return wrapper.getTranslationMemory().size();
    }

    /** @return 备份记录条数 */
    public synchronized int recordCount() {
        return wrapper.getRecords().size();
    }

    /** 记录一次翻译结果，不触碰 originalDescription */
    public synchronized void recordTranslation(String path, String translatedDescription,
                                               EffectType effect, TranslateStatus status) {
        TranslationRecord rec = get(path);
        if (rec == null) return;
        rec.setTranslatedDescription(translatedDescription);
        rec.setEffect(effect);
        rec.setStatus(status);
        rec.setLastTranslateTimestamp(System.currentTimeMillis());
        stamp(rec);
        invalidateIndexes();
        markDirty();
    }

    /** 只改状态（启动核对文件现状时使用），不动备份与译文。 */
    public synchronized void updateStatus(String path, TranslateStatus status) {
        TranslationRecord rec = get(path);
        if (rec == null || rec.getStatus() == status) return;
        rec.setStatus(status);
        markDirty();
    }

    public synchronized void recordFailure(String path) {
        TranslationRecord rec = get(path);
        if (rec != null) {
            rec.setStatus(TranslateStatus.FAILED);
            rec.setLastTranslateTimestamp(System.currentTimeMillis());
            markDirty();
        }
    }

    // ==================== 维护 ====================

    /** @return 指向已不存在文件的记录路径 */
    public synchronized List<String> orphanPaths() {
        List<String> orphans = new ArrayList<>();
        for (String path : wrapper.getRecords().keySet()) {
            if (!exists(path)) {
                orphans.add(path);
            }
        }
        return orphans;
    }

    /**
     * 把失效记录归档到独立文件（不删除：originalDescription 是原始文案的唯一副本）。
     *
     * @param archiveFile 归档文件（records.archive.json）
     * @return 归档条数
     */
    public synchronized int archiveOrphans(Path archiveFile) {
        List<String> orphans = orphanPaths();
        if (orphans.isEmpty()) {
            return 0;
        }
        try {
            RecordsWrapper archive = readArchive(archiveFile);
            for (String path : orphans) {
                archive.getRecords().put(path, wrapper.getRecords().get(path));
            }
            Path dir = archiveFile.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            String json = GSON.toJson(archive);
            Path tmp = archiveFile.resolveSibling(archiveFile.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, archiveFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("归档失效记录失败: {}", archiveFile, e);
            return 0;
        }
        for (String path : orphans) {
            wrapper.getRecords().remove(path);
        }
        invalidateIndexes();
        LOG.info("已归档 {} 条失效记录 -> {}", orphans.size(), archiveFile);
        saveToDisk();
        return orphans.size();
    }

    private RecordsWrapper readArchive(Path archiveFile) {
        try {
            if (!Files.exists(archiveFile)) return new RecordsWrapper();
            String json = Files.readString(archiveFile, StandardCharsets.UTF_8);
            RecordsWrapper w = GSON.fromJson(json, RecordsWrapper.class);
            if (w == null) return new RecordsWrapper();
            if (w.getRecords() == null) w.setRecords(new HashMap<>());
            if (w.getTranslationMemory() == null) w.setTranslationMemory(new HashMap<>());
            return w;
        } catch (IOException | RuntimeException e) {
            LOG.warn("读取归档失败，将重建: {}", archiveFile, e);
            return new RecordsWrapper();
        }
    }

    // ==================== 内容指纹 ====================

    /**
     * 补齐老记录缺失的指纹（加载后调用一次）。
     *
     * @return 补齐条数
     */
    public synchronized int fillMissingHashes() {
        int filled = 0;
        for (TranslationRecord rec : wrapper.getRecords().values()) {
            if (rec == null) continue;
            String before = String.valueOf(rec.getOriginalHash())
                    + "|" + rec.getTranslatedHash();
            stamp(rec);
            String after = String.valueOf(rec.getOriginalHash())
                    + "|" + rec.getTranslatedHash();
            if (!before.equals(after)) filled++;
        }
        if (filled > 0) {
            invalidateIndexes();
            markDirty();
        }
        return filled;
    }

    private void stamp(TranslationRecord rec) {
        if (rec == null) return;
        rec.setOriginalHash(ContentHash.of(rec.getOriginalDescription()));
        rec.setTranslatedHash(ContentHash.of(rec.getTranslatedDescription()));
    }

    /**
     * 找"这条译文是谁翻出来的"：内容必须逐字相同，且那条记录的原文与译文不同
     * （源语言与目标语言相同时 original == translated，不能当原文来源）。
     * 优先选源文件已经不存在的记录（真正的移动/改名）。
     */
    private TranslationRecord findTranslationDonor(String translatedText, String excludePath) {
        String hash = ContentHash.of(translatedText);
        if (hash.isEmpty()) return null;
        List<TranslationRecord> candidates = translationIndex().get(hash);
        if (candidates == null || candidates.isEmpty()) return null;
        TranslationRecord best = null;
        for (TranslationRecord rec : candidates) {
            if (rec == null || rec.getPath() == null || rec.getPath().equals(excludePath)) continue;
            if (rec.getStatus() != TranslateStatus.TRANSLATED) continue;
            if (!hasOriginal(rec) || rec.isOriginalUnknown()) continue;
            if (hash.equals(rec.getOriginalHash())) continue;
            if (!hash.equals(rec.getTranslatedHash())) continue;
            boolean gone = !exists(rec.getPath());
            if (best == null || (gone && exists(best.getPath()))) {
                best = rec;
            }
        }
        return best;
    }

    private Map<String, List<TranslationRecord>> translationIndex() {
        if (translationIndex != null) return translationIndex;
        Map<String, List<TranslationRecord>> index = new HashMap<>();
        for (TranslationRecord rec : wrapper.getRecords().values()) {
            if (rec == null) continue;
            String hash = rec.getTranslatedHash();
            if (hash == null || hash.isEmpty()) continue;
            index.computeIfAbsent(hash, k -> new ArrayList<>(2)).add(rec);
        }
        translationIndex = index;
        return index;
    }

    private Map<String, String> canonicalPathIndex() {
        if (canonicalPathIndex != null) return canonicalPathIndex;
        Map<String, String> index = new HashMap<>();
        for (String path : wrapper.getRecords().keySet()) {
            String canonical = canonicalOf(path);
            if (canonical != null) {
                index.putIfAbsent(canonical, path);
            }
        }
        canonicalPathIndex = index;
        return index;
    }

    private void invalidateIndexes() {
        translationIndex = null;
        canonicalPathIndex = null;
    }

    private static String canonicalOf(String path) {
        try {
            return Paths.get(path).toFile().getCanonicalPath();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean exists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean hasOriginal(TranslationRecord rec) {
        return rec != null && rec.getOriginalDescription() != null
                && !rec.getOriginalDescription().isEmpty();
    }

    /** 用户清空过备份、且内容没变时，不要自动再采用别的记录的原文。 */
    private boolean isSuppressed(String path, String currentDescription) {
        String hash = wrapper.getSuppressedHashes().get(path);
        return hash != null && hash.equals(ContentHash.of(currentDescription));
    }
}
