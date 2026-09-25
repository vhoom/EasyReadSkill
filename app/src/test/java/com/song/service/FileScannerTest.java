package com.song.service;

import com.song.model.ScanPath;
import com.song.model.SkillFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileScannerTest {

    @TempDir
    Path temp;

    @Test
    void onlyOutermostExactSkillMd() throws IOException {
        write(temp.resolve("bailian-cli/SKILL.md"), "parent");
        write(temp.resolve("bailian-cli/reference/skill.md"), "lower");
        write(temp.resolve("bailian-cli/reference/nested/SKILL.md"), "inner-upper");

        write(temp.resolve("bailian-managed-agent/SKILL.md"), "agent");
        write(temp.resolve("bailian-managed-agent/bailian-protocol/SKILL.md"), "protocol");

        write(temp.resolve("only-lower/skill.md"), "lower-root");
        write(temp.resolve("mixed-case/Skill.md"), "mixed");
        write(temp.resolve("sibling/SKILL.md"), "sibling");
        write(temp.resolve("sibling/notes.md"), "not a skill");

        List<String> found = scan(temp);

        assertEquals(3, found.size());
        assertTrue(contains(found, "bailian-cli/SKILL.md"));
        assertTrue(contains(found, "bailian-managed-agent/SKILL.md"));
        assertTrue(contains(found, "sibling/SKILL.md"));
        assertFalse(contains(found, "reference/skill.md"));
        assertFalse(contains(found, "reference/nested/SKILL.md"));
        assertFalse(contains(found, "bailian-protocol/SKILL.md"));
        assertFalse(contains(found, "only-lower/skill.md"));
        assertFalse(contains(found, "mixed-case/Skill.md"));
    }

    @Test
    void scanningInnerDirectoryDoesNotPromoteNestedSkill() throws IOException {
        write(temp.resolve("bailian-cli/SKILL.md"), "parent");
        write(temp.resolve("bailian-cli/reference/skill.md"), "lower");
        write(temp.resolve("bailian-cli/reference/nested/SKILL.md"), "inner");

        assertTrue(scan(temp.resolve("bailian-cli/reference")).isEmpty());
        assertFalse(FileScanner.isOutermostSkillFile(
                temp.resolve("bailian-cli/reference/nested/SKILL.md").toFile()));
        assertTrue(FileScanner.isOutermostSkillFile(
                temp.resolve("bailian-cli/SKILL.md").toFile()));
    }

    @Test
    void pathSpelledAsSkillMdDoesNotMatchLowercaseFile() throws IOException {
        write(temp.resolve("only-lower/skill.md"), "lower");

        File alias = temp.resolve("only-lower/SKILL.md").toFile();
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            assertTrue(alias.isFile());
        }
        assertFalse(FileScanner.isOutermostSkillFile(alias));

        assertTrue(scan(temp.resolve("only-lower")).isEmpty());
    }

    @Test
    @Timeout(5)
    void directoryCycleDoesNotRescan() throws IOException {
        write(temp.resolve("root/skillroot/SKILL.md"), "root");
        Path link = temp.resolve("root/child/loop");
        Files.createDirectories(link.getParent());
        try {
            Files.createSymbolicLink(link, temp.resolve("root"));
        } catch (IOException | UnsupportedOperationException e) {
            return;
        }

        List<String> found = scan(temp);
        assertEquals(1, found.size());
        assertTrue(contains(found, "root/skillroot/SKILL.md"));
    }

    @Test
    void lowercaseParentDoesNotHideChildSkillMd() throws IOException {
        write(temp.resolve("loose/skill.md"), "not a root");
        write(temp.resolve("loose/child/SKILL.md"), "real");

        List<String> found = scan(temp);
        assertEquals(1, found.size());
        assertTrue(contains(found, "loose/child/SKILL.md"));
    }

    @Test
    void disabledPathAndDuplicateRoots() throws IOException {
        write(temp.resolve("a/SKILL.md"), "a");
        write(temp.resolve("a/b/SKILL.md"), "b");

        List<SkillFile> none = FileScanner.scan(List.of(new ScanPath(temp.toString(), false)));
        assertTrue(none.isEmpty());

        List<SkillFile> twice = FileScanner.scan(List.of(
                new ScanPath(temp.toString(), true),
                new ScanPath(temp.resolve("a").toString(), true)));
        assertEquals(1, twice.size());
        assertTrue(twice.get(0).getFilePath().replace('\\', '/').endsWith("a/SKILL.md"));
    }

    @Test
    void externalSkillRules() throws IOException {
        File root = temp.toFile();
        Path inSkills = temp.resolve("skills/demo/SKILL.md");
        Path inSkillsNested = temp.resolve("skills/engineering/ask-matt/SKILL.md");
        Path deepPluginCache = temp.resolve(
                "plugins/cache/openai-bundled/computer-use/26.917.71314/skills/computer-use/SKILL.md");
        Path deepPluginCache2 = temp.resolve("plugins/cache/official/1.2.3/skills/engineering/triage/SKILL.md");
        Path skillsNotFirst = temp.resolve("collection/skills/in-progress/loop-me/SKILL.md");
        Path upperSkills = temp.resolve("Skills/Upper/SKILL.md");
        Path secondLevel = temp.resolve("loose/inner/SKILL.md");
        Path firstLevel = temp.resolve("loose/SKILL.md");
        Path thirdLevel = temp.resolve("loose/a/b/SKILL.md");
        for (Path p : List.of(inSkills, inSkillsNested, deepPluginCache, deepPluginCache2,
                skillsNotFirst, upperSkills, secondLevel, firstLevel, thirdLevel)) {
            write(p, "x");
        }

        // 正常：第一级是 skills（skills/<技能名>、skills/<分类>/<技能名>）
        assertTrue(FileScanner.isExternalSkill(root, inSkills.toFile()));
        assertTrue(FileScanner.isExternalSkill(root, inSkillsNested.toFile()));
        assertTrue(FileScanner.isExternalSkill(root, upperSkills.toFile()));
        // 正常：扫描根下二级目录
        assertTrue(FileScanner.isExternalSkill(root, secondLevel.toFile()));
        // 非外部：skills 不在第一级（插件缓存 / 集合目录）、只有一级、三级以上
        assertFalse(FileScanner.isExternalSkill(root, deepPluginCache.toFile()));
        assertFalse(FileScanner.isExternalSkill(root, deepPluginCache2.toFile()));
        assertFalse(FileScanner.isExternalSkill(root, skillsNotFirst.toFile()));
        assertFalse(FileScanner.isExternalSkill(root, firstLevel.toFile()));
        assertFalse(FileScanner.isExternalSkill(root, thirdLevel.toFile()));
    }

    @Test
    void scanMarksNonExternalSkills() throws IOException {
        write(temp.resolve("skills/good/SKILL.md"), "x");
        write(temp.resolve("two/levels/SKILL.md"), "x");
        write(temp.resolve("shallow/SKILL.md"), "x");
        write(temp.resolve("deep/a/b/SKILL.md"), "x");

        List<SkillFile> found = FileScanner.scan(List.of(new ScanPath(temp.toString(), true)));
        assertEquals(4, found.size());
        for (SkillFile sf : found) {
            String slash = sf.getFilePath().replace('\\', '/');
            boolean expected = slash.endsWith("/skills/good/SKILL.md")
                    || slash.endsWith("/two/levels/SKILL.md");
            assertEquals(expected, sf.isExternal(), sf.getFilePath());
        }
    }

    @Test
    void multiRootOverloadMatchesAnyRoot() throws IOException {
        write(temp.resolve("shallow/SKILL.md"), "x");
        List<ScanPath> roots = List.of(new ScanPath(temp.toString(), true));

        assertFalse(FileScanner.isExternalSkill(roots,
                temp.resolve("shallow/SKILL.md").toString()));
        // 不在任何扫描根下：无从判断，不打扰用户
        assertTrue(FileScanner.isExternalSkill(roots,
                temp.resolve("../outside-anything/SKILL.md").toString()));
    }

    @Test
    void userSkillsTreeIfPresent() {
        Path root = Path.of(System.getProperty("user.home"), ".agents", "skills");
        if (!Files.isDirectory(root)) return;

        List<SkillFile> found = FileScanner.scan(List.of(new ScanPath(root.toString(), true)));
        assertFalse(found.isEmpty(), "expected skills under " + root);
        for (SkillFile sf : found) {
            String slash = sf.getFilePath().replace('\\', '/');
            assertTrue(slash.endsWith("/SKILL.md"), slash);
            assertFalse(slash.endsWith("/skill.md"), slash);
            assertFalse(slash.contains("/reference/skill.md"), slash);
            assertFalse(slash.endsWith("/bailian-protocol/SKILL.md"), slash);
            assertTrue(FileScanner.isOutermostSkillFile(new java.io.File(sf.getFilePath())), slash);
        }
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private static List<String> scan(Path root) {
        return FileScanner.scan(List.of(new ScanPath(root.toString(), true))).stream()
                .map(sf -> sf.getFilePath().replace('\\', '/'))
                .toList();
    }

    private static boolean contains(List<String> paths, String suffix) {
        String norm = suffix.replace('\\', '/');
        return paths.stream().anyMatch(p -> p.endsWith(norm));
    }
}
