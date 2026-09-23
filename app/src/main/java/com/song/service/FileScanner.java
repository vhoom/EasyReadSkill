package com.song.service;

import com.song.model.ScanPath;
import com.song.model.SkillFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 只认文件名恰好为 SKILL.md（大小写敏感）的最外层 skill 根。
 * 某目录一旦出现该文件，不再进入其子目录。
 */
public class FileScanner {

    public static final String SKILL_FILENAME = "SKILL.md";

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", ".svn", ".hg", ".idea", "node_modules",
            "target", "build", ".gradle", ".vscode"
    );

    /** 扫描所有已启用的路径，返回合并后的 SKILL.md 列表 */
    public static List<SkillFile> scan(List<ScanPath> scanPaths) {
        List<SkillFile> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> visitedDirs = new HashSet<>();
        for (ScanPath sp : scanPaths) {
            if (!sp.isEnabled()) continue;
            File dir = new File(sp.getPath());
            if (!dir.isDirectory()) continue;
            for (String abs : collectSkillPaths(dir, visitedDirs)) {
                if (seen.add(canonicalKey(abs))) {
                    result.add(new SkillFile(abs, new File(abs).getParentFile().getName()));
                }
            }
        }
        return result;
    }

    /**
     * 只收集大写 md。某目录一旦出现该文件，视为一个 skill 根，
     * 不再进入其子目录（嵌套的 skill.md / SKILL.md 一律忽略）。
     */
    static List<String> collectSkillPaths(File dir) {
        List<String> result = new ArrayList<>();
        collect(dir, result, new HashSet<>());
        return result;
    }

    private static List<String> collectSkillPaths(File dir, Set<String> visitedDirs) {
        List<String> result = new ArrayList<>();
        collect(dir, result, visitedDirs);
        return result;
    }

    private static void collect(File dir, List<String> result, Set<String> visitedDirs) {
        if (!visitedDirs.add(canonicalKey(dir.getAbsolutePath()))) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        File skill = findExactSkillFile(files);
        List<File> subdirs = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.startsWith(".") || IGNORED_DIRS.contains(name)) continue;
                subdirs.add(f);
            }
        }

        if (skill != null) {
            // 扫描起点落在内层目录时，祖先里已有 SKILL.md 的不再收录。
            if (isOutermostSkillFile(skill)) {
                result.add(skill.getAbsolutePath());
            }
            return;
        }

        for (File sub : subdirs) {
            collect(sub, result, visitedDirs);
        }
    }

    static File findExactSkillFile(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        return findExactSkillFile(dir.listFiles());
    }

    private static File findExactSkillFile(File[] files) {
        if (files == null) return null;
        for (File f : files) {
            // 必须字面等于 SKILL.md；Windows 上 skill.md 的 getName() 仍是小写，会被拒绝。
            if (f.isFile() && SKILL_FILENAME.equals(f.getName())) return f;
        }
        return null;
    }

    /**
     * 目录项文件名必须恰好是 SKILL.md，且祖先目录中没有另一份 SKILL.md。
     * 不用路径字符串的大小写：Windows 上 {@code new File(dir, "SKILL.md")}
     * 能打开实际名为 {@code skill.md} 的文件。
     */
    public static boolean isOutermostSkillFile(File skillFile) {
        if (skillFile == null) return false;
        File dir = skillFile.getParentFile();
        if (dir == null) return false;
        File exact = findExactSkillFile(dir);
        if (exact == null || !sameFile(exact, skillFile)) return false;
        for (File p = dir.getParentFile(); p != null; p = p.getParentFile()) {
            if (findExactSkillFile(p) != null) return false;
        }
        return true;
    }

    private static boolean sameFile(File a, File b) {
        try {
            return Files.isSameFile(a.toPath(), b.toPath());
        } catch (IOException e) {
            return false;
        }
    }

    private static String canonicalKey(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (java.io.IOException e) {
            return path;
        }
    }

    /** 列出 SKILL.md 所在目录的兄弟文件和文件夹（只读，用于右侧展示） */
    public static List<String> listSiblings(String skillFilePath) {
        File parent = new File(skillFilePath).getParentFile();
        List<String> names = new ArrayList<>();
        if (parent == null || !parent.isDirectory()) return names;

        File[] files = parent.listFiles();
        if (files == null) return names;

        for (File f : files) {
            String name = f.getName();
            if (SKILL_FILENAME.equals(name)) continue;
            names.add(f.isDirectory() ? name + "/" : name);
        }
        Collections.sort(names);
        return names;
    }
}
