package com.song.service;

import com.song.model.ScanPath;
import com.song.model.SkillFile;

import java.io.File;
import java.util.*;

public class FileScanner {

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", ".svn", ".hg", ".idea", "node_modules",
            "target", "build", ".gradle", ".vscode"
    );

    /** 扫描所有已启用的路径，返回合并后的 skill.md 列表 */
    public static List<SkillFile> scan(List<ScanPath> scanPaths) {
        List<SkillFile> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ScanPath sp : scanPaths) {
            if (!sp.isEnabled()) continue;
            File dir = new File(sp.getPath());
            if (!dir.isDirectory()) continue;
            scanDir(dir, result, seen);
        }
        return result;
    }

    private static void scanDir(File dir, List<SkillFile> result, Set<String> seen) {
        File[] files = dir.listFiles();
        if (files == null) return;

        // 先判断自身是否是 skill.md，处理“skill.md 所在目录”的情况
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.startsWith(".") || IGNORED_DIRS.contains(name)) continue;
                scanDir(f, result, seen);
            } else if (f.getName().equalsIgnoreCase("skill.md")) {
                String abs = f.getAbsolutePath();
                if (seen.add(abs)) {
                    result.add(new SkillFile(abs, f.getParentFile().getName()));
                }
            }
        }
    }

    /** 列出 skill.md 所在目录的兄弟文件和文件夹（只读，用于右侧展示） */
    public static List<String> listSiblings(String skillFilePath) {
        File parent = new File(skillFilePath).getParentFile();
        List<String> names = new ArrayList<>();
        if (parent == null || !parent.isDirectory()) return names;

        File[] files = parent.listFiles();
        if (files == null) return names;

        for (File f : files) {
            String name = f.getName();
            if (name.equalsIgnoreCase("skill.md")) continue;
            names.add(f.isDirectory() ? name + "/" : name);
        }
        Collections.sort(names);
        return names;
    }
}