package com.song.service;

import com.song.config.AppConfig;
import com.song.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * 一次性数据维护。
 *
 * <p>扫描路径会随插件升级变化（例如 {@code ...\1.2.3\...} → {@code ...\1.2.4\...}），
 * 老路径的记录会永久滞留。这里把它们移到 {@code records.archive.json}：
 * 只归档不删除，因为 {@code originalDescription} 是原始文案的唯一副本。</p>
 */
public final class DataMaintenance {

    private static final Logger LOG = LoggerFactory.getLogger(DataMaintenance.class);

    /** 归档文件名。 */
    public static final String ARCHIVE_NAME = "records.archive.json";

    private DataMaintenance() {}

    /**
     * 归档失效记录（只做一次，结果记在 config 里）。
     *
     * <p>归档前必须先成功备份 records.json，否则本次跳过，下次启动再试。</p>
     *
     * @param config  应用配置（读写 recordsArchived 标记）
     * @param records 记录管理器
     * @return 归档条数；未执行或没有失效记录返回 0
     */
    public static int archiveOrphanRecordsOnce(AppConfig config, RecordManager records) {
        if (config == null || records == null || config.isRecordsArchived()) {
            return 0;
        }
        List<String> orphans = records.orphanPaths();
        if (orphans.isEmpty()) {
            config.setRecordsArchived(true);
            return 0;
        }
        Path recordsFile = records.getRecordsFile();
        if (!ConfigManager.backupOnce(recordsFile, "v1")) {
            LOG.warn("records.json 备份失败，本次不归档（下次启动重试）");
            return 0;
        }
        int moved = records.archiveOrphans(recordsFile.resolveSibling(ARCHIVE_NAME));
        if (moved > 0) {
            config.setRecordsArchived(true);
            LOG.info("失效记录已归档 {} 条", moved);
        }
        return moved;
    }
}
