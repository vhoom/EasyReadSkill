package com.song.service;

import com.song.model.EffectType;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SkillFileService {

    private static final Logger LOG = LoggerFactory.getLogger(SkillFileService.class);

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
        Files.writeString(Paths.get(filePath), content, StandardCharsets.UTF_8);
    }

    /** 首次备份：只要 records 里没有原始 Description，就写入 */
    public void ensureBackup(String filePath) throws IOException {
        TranslationRecord rec = recordManager.get(filePath);
        if (rec != null && rec.getOriginalDescription() != null
                && !rec.getOriginalDescription().isEmpty()) {
            return;
        }
        String content = readFile(filePath);
        String original = SkillParser.extractDescription(content);
        if (original == null) return;
        recordManager.recordFirstBackup(filePath, original);
    }

    /**
     * 重新备份：强制使用当前文件中的 Description 覆盖已有备份。
     * 适合之前因解析错误导致备份内容不正确时修复。
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
            LOG.info("重新备份成功: {}", filePath);
            return true;
        } catch (Exception e) {
            LOG.error("重新备份异常: {}", filePath, e);
            return false;
        }
    }

    /**
     * 翻译一个文件并写回。
     */
    public TranslationResult translateFile(String filePath, EffectType effect,
                                           String from, String to) {
        try {
            ensureBackup(filePath);

            String content = readFile(filePath);
            String original = SkillParser.extractDescription(content);
            if (original == null) {
                String error = "未找到 Description 字段";
                LOG.warn("翻译失败，{}: {}", error, filePath);
                recordManager.recordFailure(filePath);
                return TranslationResult.failure(error);
            }

            // 翻译+原文以及重复翻译时，都以首次备份的原文作为源文本。
            String backupOriginal = recordManager.getOriginalDescription(filePath);
            String sourceText = (backupOriginal != null && !backupOriginal.isEmpty())
                    ? backupOriginal : original;

            String cacheKey = buildCacheKey(sourceText, from, to);
            String translated = recordManager.getCachedTranslation(cacheKey);
            boolean fromCache = translated != null;
            if (!fromCache) {
                TranslationResult result = translationService.translate(sourceText, from, to);
                if (!result.isSuccess()) {
                    LOG.warn("翻译服务返回失败: {} - {}", filePath, result.getErrorMessage());
                    recordManager.recordFailure(filePath);
                    return result;
                }
                translated = result.getText();
                recordManager.putCachedTranslation(cacheKey, translated);
            }

            String newDesc = (effect == EffectType.OVERWRITE)
                    ? translated
                    : translated + "/" + sourceText;

            String newContent = SkillParser.replaceDescription(content, newDesc);
            writeFile(filePath, newContent);

            recordManager.recordTranslation(filePath, newDesc, effect, TranslateStatus.TRANSLATED);
            LOG.info(fromCache ? "翻译命中缓存: {}" : "翻译成功: {}", filePath);
            return TranslationResult.success(newDesc);

        } catch (Exception e) {
            LOG.error("翻译异常: {}", filePath, e);
            recordManager.recordFailure(filePath);
            return TranslationResult.failure(e.getMessage() == null
                    ? "翻译异常" : e.getMessage());
        }
    }

    private String buildCacheKey(String original, String from, String to) {
        String provider = translationService.getProvider() != null
                ? translationService.getProvider().getType().getId()
                : "unknown";
        return provider + "|" + from + "|" + to + "|" + original;
    }

    /** 还原最初：把 originalDescription 写回文件 */
    public boolean restoreFile(String filePath) {
        try {
            String original = recordManager.getOriginalDescription(filePath);
            if (original == null) {
                LOG.warn("还原失败，无原始备份: {}", filePath);
                return false;
            }
            String content = readFile(filePath);
            String newContent = SkillParser.replaceDescription(content, original);
            writeFile(filePath, newContent);

            recordManager.recordTranslation(filePath, null, null, TranslateStatus.UNTRANSLATED);
            LOG.info("还原成功: {}", filePath);
            return true;
        } catch (Exception e) {
            LOG.error("还原异常: {}", filePath, e);
            return false;
        }
    }
}