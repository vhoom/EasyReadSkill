package com.song.service;

import com.song.model.EffectType;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import com.song.util.ContentHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordManagerTest {

    @TempDir
    Path temp;

    @Test
    void writesOnlyWhenFlushed() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(skill.toString(), "Hello");

        // 只置脏标记，批处理中间不落盘
        assertFalse(Files.exists(temp.resolve("records.json")));
        rm.flush();
        assertTrue(Files.exists(temp.resolve("records.json")));

        RecordManager reloaded = new RecordManager(temp);
        assertEquals("Hello", reloaded.getOriginalDescription(skill.toString()));
    }

    @Test
    void rebackupKeepsPreviousCopyForRollback() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(skill.toString(), "Original");
        rm.rebackup(skill.toString(), "Second");

        TranslationRecord rec = rm.get(skill.toString());
        assertEquals("Second", rec.getOriginalDescription());
        assertEquals("Original", rec.getPreviousOriginalDescription());
        assertTrue(rec.getPreviousOriginalTimestamp() > 0);
    }

    @Test
    void movedFileInheritsOriginalInsteadOfTranslation() throws Exception {
        Path oldFile = writeSkill(temp.resolve("old/SKILL.md"));
        Path newFile = writeSkill(temp.resolve("new/SKILL.md"));

        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(oldFile.toString(), "Original text");
        rm.recordTranslation(oldFile.toString(), "中文译文", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);

        // 技能升级到新路径后文件里已经是译文，此时首次备份必须继承旧原文
        TranslationRecord rec = rm.recordFirstBackup(newFile.toString(), "中文译文");
        assertEquals("Original text", rec.getOriginalDescription());
    }

    @Test
    void firstBackupUsesCurrentTextWhenNothingToInherit() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        TranslationRecord rec = rm.recordFirstBackup(skill.toString(), "Brand new text");
        assertEquals("Brand new text", rec.getOriginalDescription());
    }

    @Test
    void archiveOrphansMovesOnlyMissingFiles() throws Exception {
        Path live = writeSkill(temp.resolve("live/SKILL.md"));
        String gonePath = temp.resolve("gone/SKILL.md").toString();

        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(live.toString(), "L");
        rm.recordFirstBackup(gonePath, "G");
        rm.flush();

        List<String> orphans = rm.orphanPaths();
        assertEquals(1, orphans.size());
        assertEquals(gonePath, orphans.get(0));

        Path archive = temp.resolve("records.archive.json");
        assertEquals(1, rm.archiveOrphans(archive));

        assertTrue(rm.orphanPaths().isEmpty());
        assertEquals("L", rm.getOriginalDescription(live.toString()));
        // 归档文件里原始备份必须还在
        assertTrue(Files.readString(archive, StandardCharsets.UTF_8).contains("G"));
    }

    @Test
    void legacyCacheKeyIsPromotedToNewKey() throws Exception {
        RecordManager rm = new RecordManager(temp);
        String legacy = "youdao_text|en|zh|Hello";
        String fresh = "v2|youdao_text||none|en|zh|Hello";
        rm.putCachedTranslation(legacy, "你好");

        assertEquals("你好", rm.getCachedTranslation(fresh, legacy));
        // 命中旧键后应顺手升级到新键
        assertEquals("你好", rm.getCachedTranslation(fresh));
    }

    @Test
    void brokenFileIsQuarantinedInsteadOfCrashing() throws Exception {
        Path records = temp.resolve("records.json");
        Files.writeString(records, "{ not json at all", StandardCharsets.UTF_8);

        RecordManager rm = new RecordManager(temp);
        assertTrue(rm.orphanPaths().isEmpty());
        assertTrue(Files.exists(temp.resolve("records.json.bad")));
    }

    @Test
    void markTranslatedWithoutDonorHasNoOriginal() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);

        TranslationRecord rec = rm.markTranslated(skill.toString(), "这是别人翻好的中文");

        assertEquals(TranslateStatus.TRANSLATED, rec.getStatus());
        assertEquals("这是别人翻好的中文", rec.getTranslatedDescription());
        assertTrue(rec.isOriginalUnknown());
    }

    @Test
    void markTranslatedAdoptsOriginalFromSameContent() throws Exception {
        Path donorFile = writeSkill(temp.resolve("donor/SKILL.md"));
        Path targetFile = writeSkill(temp.resolve("target/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(donorFile.toString(), "English original");
        rm.recordTranslation(donorFile.toString(), "中文译文", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);

        TranslationRecord rec = rm.markTranslated(targetFile.toString(), "中文译文");

        assertEquals("English original", rec.getOriginalDescription());
        assertEquals(donorFile.toString(), rec.getOriginalSourcePath());
        assertFalse(rec.isOriginalUnknown());
    }

    @Test
    void duplicateDirectoryAdoptsKnownTranslation() throws Exception {
        Path donorFile = writeSkill(temp.resolve("rootA/SKILL.md"));
        Path copyFile = writeSkill(temp.resolve("rootB/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(donorFile.toString(), "English original");
        rm.recordTranslation(donorFile.toString(), "中文译文", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);

        // 另一个扫描目录里的同一份技能，内容已经是译文
        TranslationRecord adopted = rm.adoptKnownTranslation(copyFile.toString(), "中文译文");

        assertNotNull(adopted);
        assertEquals(TranslateStatus.TRANSLATED, adopted.getStatus());
        assertEquals("English original", adopted.getOriginalDescription());
    }

    @Test
    void clearedBackupSuppressesAdoptionUntilContentChanges() throws Exception {
        Path donorFile = writeSkill(temp.resolve("donor/SKILL.md"));
        Path otherFile = writeSkill(temp.resolve("other/SKILL.md"));
        Path targetFile = writeSkill(temp.resolve("target/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(donorFile.toString(), "English A");
        rm.recordTranslation(donorFile.toString(), "译文A", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);
        rm.recordFirstBackup(otherFile.toString(), "English B");
        rm.recordTranslation(otherFile.toString(), "译文B", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);

        // 清空备份后，内容没变就不再自动采用
        rm.clearBackup(targetFile.toString(), "译文A");
        assertNull(rm.adoptKnownTranslation(targetFile.toString(), "译文A"));

        // 内容换成另一段，压制自动失效
        assertNotNull(rm.adoptKnownTranslation(targetFile.toString(), "译文B"));
    }

    @Test
    void samePhysicalFileIsFoundThroughAliasPath() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(skill.toString(), "Hello");

        String alias = temp.resolve("a/../a/SKILL.md").toString();
        TranslationRecord rec = rm.get(alias);

        assertNotNull(rec);
        assertEquals("Hello", rec.getOriginalDescription());
        assertEquals(alias, rec.getPath());
    }

    @Test
    void fillMissingHashesBackfillsLegacyRecords() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        String json = "{\"records\":{\"" + skill.toString().replace("\\", "\\\\")
                + "\":{\"path\":\"" + skill.toString().replace("\\", "\\\\")
                + "\",\"originalDescription\":\"EN\",\"translatedDescription\":\"ZH\","
                + "\"status\":\"TRANSLATED\"}},\"translationMemory\":{}}";
        Files.writeString(temp.resolve("records.json"), json, StandardCharsets.UTF_8);

        RecordManager rm = new RecordManager(temp);
        assertEquals(1, rm.fillMissingHashes());

        TranslationRecord rec = rm.get(skill.toString());
        assertEquals(ContentHash.of("EN"), rec.getOriginalHash());
        assertEquals(ContentHash.of("ZH"), rec.getTranslatedHash());
    }

    @Test
    void clearAllCanKeepTranslationMemory() throws Exception {
        Path skill = writeSkill(temp.resolve("a/SKILL.md"));
        RecordManager rm = new RecordManager(temp);
        rm.recordFirstBackup(skill.toString(), "EN");
        rm.recordTranslation(skill.toString(), "ZH", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);
        rm.putCachedTranslation("k", "v");

        assertEquals(1, rm.recordCount());
        assertEquals(1, rm.translationMemorySize());

        assertEquals(1, rm.clearAll(true));
        assertEquals(0, rm.recordCount());
        assertEquals(1, rm.translationMemorySize());

        rm.putCachedTranslation("k2", "v2");
        rm.clearAll(false);
        assertEquals(0, rm.translationMemorySize());
    }

    private static Path writeSkill(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "---\ndescription: text\n---\n", StandardCharsets.UTF_8);
        return file;
    }
}
