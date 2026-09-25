package com.song.service;

import com.song.config.AppConfig;
import com.song.model.EffectType;
import com.song.model.TranslateStatus;
import com.song.model.TranslationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillFileServiceTest {

    @TempDir
    Path temp;

    /** 应用里只有一个 RecordManager 实例，测试也要共用同一个。 */
    private RecordManager records;
    private SkillFileService svc;

    @BeforeEach
    void setUp() {
        records = new RecordManager(temp);
        svc = new SkillFileService(records, new TranslationService(new AppConfig()));
    }

    @Test
    void noConflictBeforeFirstBackup() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "Hello");
        assertFalse(svc.hasConflict(skill.toString()));
    }

    @Test
    void conflictWhenFileChangedSinceBackup() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "Hello");
        records.recordFirstBackup(skill.toString(), "Hello");
        assertFalse(svc.hasConflict(skill.toString()));

        write(skill, "Hello updated upstream");
        assertTrue(svc.hasConflict(skill.toString()));
    }

    @Test
    void conflictComparedAgainstLastWrittenTranslation() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "Hello");
        records.recordFirstBackup(skill.toString(), "Hello");
        records.recordTranslation(skill.toString(), "你好", EffectType.OVERWRITE,
                TranslateStatus.TRANSLATED);

        write(skill, "你好");
        assertFalse(svc.hasConflict(skill.toString()));

        write(skill, "别人改过的内容");
        assertTrue(svc.hasConflict(skill.toString()));
    }

    @Test
    void translateStopsOnConflictUnlessUserRebackups() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "Hello");
        records.recordFirstBackup(skill.toString(), "Old original from before the update");

        TranslationResult stopped = svc.translateFile(
                skill.toString(), EffectType.OVERWRITE, "en", "zh", true, false);
        assertTrue(stopped.isConflict());
        // 冲突时不得改动文件内容
        String onDisk = Files.readString(skill, StandardCharsets.UTF_8);
        assertTrue(onDisk.contains("Hello"), onDisk);

        // 用户选择"按当前文件重新备份"：备份更新为当前内容，不再算冲突
        TranslationResult proceeded = svc.translateFile(
                skill.toString(), EffectType.OVERWRITE, "en", "zh", true, true);
        assertFalse(proceeded.isConflict());
        assertEquals("Hello", records.getOriginalDescription(skill.toString()));
        assertEquals("Old original from before the update",
                records.get(skill.toString()).getPreviousOriginalDescription());
    }

    @Test
    void firstTranslateBacksUpCurrentTextOnly() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "Fresh text");
        // 没有密钥，翻译必然失败，但"备份 = 即将被覆盖的内容"必须已经建立
        TranslationResult result = svc.translateFile(
                skill.toString(), EffectType.OVERWRITE, "en", "zh", true, false);

        assertFalse(result.isConflict());
        assertEquals("Fresh text", records.getOriginalDescription(skill.toString()));
    }

    @Test
    void keepEnglishDividerMovesToOwnLineWhenMultiline() {
        assertEquals("译A\n译B\n\n/\n原文",
                SkillFileService.composeKeepEnglish("译A\n译B", "原文"));
        assertEquals("译文/原文", SkillFileService.composeKeepEnglish("译文", "原文"));
    }

    @Test
    void markTranslatedKeepsFileContentUntouched() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "别人翻好的中文");
        TranslationRecord rec = svc.markTranslatedFile(skill.toString());

        assertNotNull(rec);
        assertEquals(TranslateStatus.TRANSLATED, rec.getStatus());
        assertEquals("别人翻好的中文", rec.getTranslatedDescription());
        assertTrue(Files.readString(skill, StandardCharsets.UTF_8).contains("别人翻好的中文"));
        assertFalse(svc.hasConflict(skill.toString()));
    }

    @Test
    void restoreIsRefusedWhenOriginalIsUnknown() throws Exception {
        Path skill = write(temp.resolve("a/SKILL.md"), "别人翻好的中文");
        svc.markTranslatedFile(skill.toString());

        assertTrue(svc.hasUnknownOriginal(skill.toString()));
        assertFalse(svc.restoreFile(skill.toString()));
        assertTrue(Files.readString(skill, StandardCharsets.UTF_8).contains("别人翻好的中文"));
    }

    @Test
    void duplicatedDirectoryStateIsNotMisjudged() throws Exception {
        // rootA 已翻译，rootB 是内容相同的副本
        Path rootA = write(temp.resolve("rootA/SKILL.md"), "English original");
        Path rootB = write(temp.resolve("rootB/SKILL.md"), "中文译文");
        records.recordFirstBackup(rootA.toString(), "English original");
        records.recordTranslation(rootA.toString(), "中文译文",
                com.song.model.EffectType.OVERWRITE, TranslateStatus.TRANSLATED);

        TranslationRecord adopted = records.adoptKnownTranslation(rootB.toString(), "中文译文");

        assertNotNull(adopted);
        assertEquals("English original", adopted.getOriginalDescription());
        // 采用之后 rootB 既不算冲突，也不会被当成"未翻译"
        assertFalse(svc.hasConflict(rootB.toString()));
    }

    private static Path write(Path file, String description) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "---\ndescription: " + description + "\n---\n",
                StandardCharsets.UTF_8);
        return file;
    }
}
