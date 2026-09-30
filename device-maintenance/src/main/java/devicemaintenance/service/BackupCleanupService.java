package devicemaintenance.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.rpc.client.dto.ModuleCredential;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClient;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.utils.ModuleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过期备份清理服务
 * 
 * 功能：
 * - 清理超过保留期限的备份文件
 * - 保护机制：每个设备至少保留一个有效备份（非空目录）
 * - 只删除文件，空目录保留（SFTP协议限制）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BackupCleanupService {

    @Autowired(required = false)
    private OdlRpcClient odlRpcClient;

    @Value("${device-maintenance.network-backup.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${device-maintenance.network-backup.cleanup.retention-days:7}")
    private int retentionDays;

    /**
     * 清理过期备份
     *
     * @param sftpServerName SFTP服务器名称
     * @param basePath       备份根路径
     * @return 清理结果
     */
    public CleanupResult cleanupExpiredBackups(String sftpServerName, String basePath) {
        if (!cleanupEnabled) {
            log.info("🧹 过期备份清理功能已禁用，跳过");
            return CleanupResult.disabled();
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 开始清理过期备份");
        log.info("  SFTP服务器: {}", sftpServerName);
        log.info("  备份根路径: {}", basePath);
        log.info("  保留天数: {} 天", retentionDays);

        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        log.info("  过期阈值: {} (此日期之前的将被清理)", cutoffDate);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int totalFilesDeleted = 0;
        int totalDirsProcessed = 0;
        int totalDevicesProtected = 0;
        int totalErrors = 0;

        try {
            // 获取 ftpServer 服务地址
            ModuleCredential credential = ModuleUtils.getCredential("ftpServer");
            if (credential == null) {
                log.error("❌ ftpServer 服务不可用");
                return CleanupResult.error("ftpServer service not available");
            }

            // 正确处理 IPv6 地址（需要用方括号括起来）
            String ip = CommonUtil.formateIpAddress(credential.getIp());
            String listUrl = String.format("http://%s:%d/restconf/operations/ftp-server:list",
                    ip, credential.getPort());
            String rmUrl = String.format("http://%s:%d/restconf/operations/ftp-server:rm",
                    ip, credential.getPort());

            // 1. 列出 basePath 下所有设备目录
            List<FileInfo> deviceDirs = listDirectory(listUrl, sftpServerName, basePath);
            log.info("📂 找到 {} 个设备目录", deviceDirs.size());

            for (FileInfo deviceDir : deviceDirs) {
                if (!deviceDir.isDirectory()) {
                    continue;
                }

                String deviceName = deviceDir.getName();
                String devicePath = basePath + "/" + deviceName;

                log.info("────────────────────────────────────────");
                log.info("📁 处理设备: {}", deviceName);

                try {
                    // 2. 列出该设备的所有日期目录
                    List<FileInfo> dateDirs = listDirectory(listUrl, sftpServerName, devicePath);

                    // 3. 分析每个日期目录（检查是否有文件）
                    List<DateDirInfo> dateDirInfos = new ArrayList<>();
                    for (FileInfo dateDir : dateDirs) {
                        if (!dateDir.isDirectory()) {
                            continue;
                        }

                        String dateDirName = dateDir.getName();
                        LocalDate dirDate = parseDateDirectory(dateDirName);
                        if (dirDate == null) {
                            log.debug("  跳过非日期目录: {}", dateDirName);
                            continue;
                        }

                        // 检查目录内是否有文件
                        String dateDirPath = devicePath + "/" + dateDirName;
                        List<FileInfo> files = listDirectory(listUrl, sftpServerName, dateDirPath);
                        List<FileInfo> backupFiles = files.stream()
                                .filter(f -> !f.isDirectory() && f.getName().endsWith(".db"))
                                .collect(Collectors.toList());

                        dateDirInfos.add(new DateDirInfo(dateDirName, dirDate, dateDirPath, backupFiles));
                    }

                    // 4. 按日期排序（最新的在前）
                    dateDirInfos.sort(Comparator.comparing(DateDirInfo::getDate).reversed());

                    // 5. 筛选有效备份（非空目录）
                    List<DateDirInfo> validBackups = dateDirInfos.stream()
                            .filter(d -> !d.getFiles().isEmpty())
                            .collect(Collectors.toList());

                    log.info("  日期目录: {} 个, 有效备份: {} 个", dateDirInfos.size(), validBackups.size());

                    // 6. 确定要清理的目录
                    List<DateDirInfo> toCleanup = new ArrayList<>();
                    for (DateDirInfo dirInfo : dateDirInfos) {
                        boolean isExpired = dirInfo.getDate().isBefore(cutoffDate);
                        boolean hasFiles = !dirInfo.getFiles().isEmpty();

                        if (isExpired && hasFiles) {
                            // 保护检查：如果这是最后一个有效备份，不删除
                            long remainingValidBackups = validBackups.stream()
                                    .filter(d -> !toCleanup.contains(d))
                                    .count();

                            if (remainingValidBackups <= 1) {
                                log.warn("  ⚠️ 保护: {} 是最后一个有效备份，跳过", dirInfo.getDirName());
                                totalDevicesProtected++;
                                break; // 后面的更旧，也不删了
                            }

                            toCleanup.add(dirInfo);
                        }
                    }

                    // 7. 执行清理
                    for (DateDirInfo dirInfo : toCleanup) {
                        log.info("  🗑️ 清理: {} ({} 个文件)", dirInfo.getDirName(), dirInfo.getFiles().size());

                        for (FileInfo file : dirInfo.getFiles()) {
                            try {
                                deleteFile(rmUrl, sftpServerName, dirInfo.getPath(), file.getName());
                                totalFilesDeleted++;
                                log.debug("    ✓ 删除: {}", file.getName());
                            } catch (Exception e) {
                                totalErrors++;
                                log.error("    ✗ 删除失败: {} - {}", file.getName(), e.getMessage());
                            }
                        }
                        totalDirsProcessed++;
                    }

                } catch (Exception e) {
                    log.error("  ❌ 处理设备 {} 失败: {}", deviceName, e.getMessage());
                    totalErrors++;
                }
            }

        } catch (Exception e) {
            log.error("❌ 清理过期备份失败: {}", e.getMessage(), e);
            return CleanupResult.error(e.getMessage());
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 清理完成");
        log.info("  删除文件: {} 个", totalFilesDeleted);
        log.info("  处理目录: {} 个", totalDirsProcessed);
        log.info("  保护设备: {} 个（只剩一个有效备份）", totalDevicesProtected);
        log.info("  错误数: {}", totalErrors);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new CleanupResult(totalFilesDeleted, totalDirsProcessed, totalDevicesProtected, totalErrors, null);
    }

    /**
     * 列出目录内容
     */
    private List<FileInfo> listDirectory(String url, String serverName, String path) {
        List<FileInfo> result = new ArrayList<>();

        try {
            String requestJson = String.format(
                    "{\"input\":{\"server-name\":\"%s\",\"folder-name\":\"%s\"}}",
                    serverName, path);

            odlRpcClient.setConfig(new OdlRpcClientConfig.Builder()
                    .user("admin").password("admin").build());
            String responseBody = odlRpcClient.post(url, requestJson);

            if (responseBody == null || responseBody.isEmpty()) {
                return result;
            }

            JSONObject json = JSON.parseObject(responseBody);
            JSONObject output = json.getJSONObject("output");
            if (output == null) {
                return result;
            }

            // 尝试解析 file 数组
            JSONArray fileArray = output.getJSONArray("file");
            if (fileArray != null) {
                for (int i = 0; i < fileArray.size(); i++) {
                    JSONObject fileObj = fileArray.getJSONObject(i);
                    String name = fileObj.getString("name");
                    String type = fileObj.getString("type");

                    if (name != null && !name.isEmpty() && !".".equals(name) && !"..".equals(name)) {
                        boolean isDir = "directory".equalsIgnoreCase(type) || "folder".equalsIgnoreCase(type);
                        result.add(new FileInfo(name, isDir));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("列出目录失败: {} - {}", path, e.getMessage());
        }

        return result;
    }

    /**
     * 删除文件
     */
    private void deleteFile(String url, String serverName, String folder, String fileName) {
        try {
            String requestJson = String.format(
                    "{\"input\":{\"server-name\":\"%s\",\"remote-folder\":\"%s\",\"file-name\":\"%s\"}}",
                    serverName, folder, fileName);

            odlRpcClient.setConfig(new OdlRpcClientConfig.Builder()
                    .user("admin").password("admin").build());
            String responseBody = odlRpcClient.post(url, requestJson);

            // 检查响应
            if (responseBody != null) {
                JSONObject json = JSON.parseObject(responseBody);
                JSONObject output = json.getJSONObject("output");
                if (output != null) {
                    String result = output.getString("result");
                    if (!"success".equalsIgnoreCase(result)) {
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                "删除文件失败: " + responseBody);
                    }
                }
            }
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析日期目录名称
     */
    private LocalDate parseDateDirectory(String dirName) {
        try {
            return LocalDate.parse(dirName, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ========== 内部数据类 ==========

    @Data
    private static class FileInfo {
        private final String name;
        private final boolean directory;
    }

    @Data
    private static class DateDirInfo {
        private final String dirName;
        private final LocalDate date;
        private final String path;
        private final List<FileInfo> files;
    }

    @Data
    public static class CleanupResult {
        private final int filesDeleted;
        private final int dirsProcessed;
        private final int devicesProtected;
        private final int errors;
        private final String errorMessage;

        public static CleanupResult disabled() {
            return new CleanupResult(0, 0, 0, 0, "disabled");
        }

        public static CleanupResult error(String message) {
            return new CleanupResult(0, 0, 0, 1, message);
        }

        public boolean isSuccess() {
            return errorMessage == null && errors == 0;
        }
    }
}

