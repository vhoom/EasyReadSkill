package com.song.service;

import com.song.model.ScanPath;
import com.song.model.SkillFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** 视为"正常"的目录层级：<扫描根>/<一级>/<二级>/SKILL.md */
    private static final int SECOND_LEVEL_DEPTH = 2;

    /** 约定的 skills 目录名（大小写不敏感），必须是扫描根下的第一级目录。 */
    private static final String SKILLS_DIR = "skills";

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
                    SkillFile sf = new SkillFile(abs, new File(abs).getParentFile().getName());
                    sf.setExternal(isExternalSkill(dir, new File(abs)));
                    result.add(sf);
                }
            }
        }
        return result;
    }

    /**
     * 是否算"外部 skill"（列表正常显示）：
     *
     * <p>只按相对扫描根的路径判断，满足其一即可：</p>
     * <ol>
     *   <li><b>扫描根下第一级就是 {@code skills} 目录</b>（大小写不敏感），
     *       SKILL.md 在该目录之下，例如 {@code <根>/skills/<技能名>/SKILL.md}、
     *       {@code <根>/skills/<分类>/<技能名>/SKILL.md}；</li>
     *   <li>或 SKILL.md 正好在扫描根下二级目录里：{@code <根>/<一级>/<二级>/SKILL.md}。</li>
     * </ol>
     *
     * <p>其它情况算非外部，例如插件缓存里的
     * {@code <根>/plugins/cache/<插件>/<版本>/skills/<技能名>/SKILL.md}
     * （skills 不是第一级）、{@code <根>/某目录/SKILL.md}（只有一级）、
     * {@code <根>/某目录/a/b/SKILL.md}（三级以上）。</p>
     *
     * 其它情况仍然会扫描出来并列出，只是界面上标"非外部skill"。
     *
     * @param scanRoot  扫描根目录
     * @param skillFile SKILL.md
     * @return 是否按规则匹配
     */
    public static boolean isExternalSkill(File scanRoot, File skillFile) {
        if (scanRoot == null || skillFile == null) return false;
        List<String> segments = relativeSegments(scanRoot, skillFile);
        if (segments == null || segments.size() < 2) return false;
        List<String> dirs = segments.subList(0, segments.size() - 1);
        if (SKILLS_DIR.equalsIgnoreCase(dirs.get(0))) {
            return true;
        }
        return dirs.size() == SECOND_LEVEL_DEPTH;
    }

    /**
     * 在所有已配置扫描路径里判断（不区分是否启用）：任一扫描根匹配即算外部 skill。
     * 文件不在任何已配置扫描路径下时返回 true（无从判断，不打扰用户）。
     *
     * @param scanPaths      扫描路径配置
     * @param skillFilePath  SKILL.md 路径
     * @return 是否按规则匹配
     */
    public static boolean isExternalSkill(List<ScanPath> scanPaths, String skillFilePath) {
        if (skillFilePath == null) return true;
        if (scanPaths == null || scanPaths.isEmpty()) return true;
        File file = new File(skillFilePath);
        boolean underAnyRoot = false;
        for (ScanPath sp : scanPaths) {
            if (sp == null || sp.getPath() == null) continue;
            File root = new File(sp.getPath());
            if (relativeSegments(root, file) == null) continue;
            underAnyRoot = true;
            if (isExternalSkill(root, file)) return true;
        }
        return !underAnyRoot;
    }



    /**
     * @param root 根目录
     * @param file 文件
     * @return 相对路径的各段；不在根目录下返回 null
     */
    static List<String> relativeSegments(File root, File file) {
        try {
            Path r = root.toPath().toAbsolutePath().normalize();
            Path f = file.toPath().toAbsolutePath().normalize();
            if (!f.startsWith(r)) return null;
            List<String> out = new ArrayList<>();
            for (Path part : r.relativize(f)) {
                out.add(part.toString());
            }
            return out;
        } catch (RuntimeException e) {
            // 不同盘符等情况
            return null;
        }
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
