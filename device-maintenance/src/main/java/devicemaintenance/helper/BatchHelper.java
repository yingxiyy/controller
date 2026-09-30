package devicemaintenance.helper;

import devicemaintenance.entity.Batch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Batch 创建辅助工具类
 * 
 * 用于消除 Batch 创建的重复代码，提供统一的创建方法
 */
@Slf4j
public class BatchHelper {

    /**
     * 创建 Batch 实体
     * 
     * @param batchId 批次ID
     * @param batchName 批次名称
     * @param batchActionTime 批次动作时间
     * @param batchType 批次类型
     * @param deviceCount 设备数量
     * @param initialStatus 初始状态
     * @param context 批次创建上下文（可选字段）
     * @return 创建的 Batch 实体
     */
    public static Batch createBatch(
            String batchId,
            String batchName,
            Long batchActionTime,
            Batch.BatchType batchType,
            int deviceCount,
            Batch.BatchStatus initialStatus,
            BatchCreationContext context) {
        
        Batch batch = new Batch();
        batch.setBatchId(batchId);
        batch.setBatchName(batchName);
        batch.setBatchActionTime(batchActionTime);
        batch.setBatchType(batchType);
        batch.setDeviceCount(deviceCount);
        batch.setStatus(initialStatus);
        
        // ⭐ 从 HTTP request header 获取用户并设置 createdBy
        batch.setCreatedBy(getCurrentUserFromRequest());
        
        // 从上下文设置可选字段
        if (context != null) {
            batch.setBasePath(context.getBasePath());
            batch.setSftpServerName(context.getSftpServerName());
            batch.setRemark(context.getRemark());
            batch.setDebug(context.getDebug() != null && context.getDebug());
            batch.setScheduledTime(context.getScheduledTime());
            
            // Upgrade 特有字段
            batch.setFilePath(context.getFilePath());
            batch.setTargetVersion(context.getTargetVersion());
            batch.setExecutionMode(context.getExecutionMode());
            batch.setEnableDownload(context.getEnableDownload());
            batch.setEnableBackup(context.getEnableBackup());
            batch.setEnableUpgrade(context.getEnableUpgrade());
            batch.setBackupBasePath(context.getBackupBasePath());
        }
        
        return batch;
    }
    
    /**
     * 从当前 HTTP 请求获取用户名
     * Gateway 会将 JWT token 转换为用户名并放入 "user" header
     * 
     * @return 用户名，如果无法获取则返回 "system"
     */
    private static String getCurrentUserFromRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                javax.servlet.http.HttpServletRequest request = attributes.getRequest();
                // Gateway 使用 "user" header 传递用户名
                String user = request.getHeader("user");
                
                if (user != null && !user.trim().isEmpty()) {
                    return user;
                }
            }
        } catch (Exception e) {
            // 忽略异常（可能在非HTTP上下文中调用）
        }
        
        return null;  // ⭐ 无法获取时返回 null（不使用默认值）
    }
    
    /**
     * 从文件路径提取版本号
     * 
     * 提取规则：
     * 1. 去除路径前缀（只保留文件名）
     * 2. 去除文件扩展名（.tar, .tar.gz, .zip 等）
     * 3. 去除常见前缀（Release_, Package_ 等，可选）
     * 
     * 示例：
     * - /software/Release_POS_3.4.0.163_20251026.tar → POS_3.4.0.163_20251026
     * - /path/to/POS_3.4.0.150.tar.gz → POS_3.4.0.150
     * - firmware_v1.2.3.bin → firmware_v1.2.3
     * 
     * @param filePath 文件路径
     * @return 提取的版本号，如果无法提取则返回 null
     */
    public static String extractVersionFromFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 1. 提取文件名（去除路径）
            String fileName = filePath;
            int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            if (lastSlash >= 0) {
                fileName = filePath.substring(lastSlash + 1);
            }
            
            // 2. 去除扩展名
            String version = fileName;
            // 处理 .tar.gz, .tar.bz2 等双扩展名
            if (version.endsWith(".tar.gz") || version.endsWith(".tar.bz2")) {
                version = version.substring(0, version.length() - 7);
            } else if (version.endsWith(".tar.xz")) {
                version = version.substring(0, version.length() - 7);
            } else {
                // 处理单扩展名 .tar, .zip, .bin 等
                int lastDot = version.lastIndexOf('.');
                if (lastDot > 0) {
                    version = version.substring(0, lastDot);
                }
            }
            
            // 3. 去除常见前缀（可选）
            if (version.startsWith("Release_")) {
                version = version.substring(8);
            } else if (version.startsWith("Package_")) {
                version = version.substring(8);
            } else if (version.startsWith("Software_")) {
                version = version.substring(9);
            }
            
            log.debug("从文件路径提取版本号: {} → {}", filePath, version);
            return version.isEmpty() ? null : version;
            
        } catch (Exception e) {
            log.warn("提取版本号失败: filePath={}, error={}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * 批次创建上下文
     * 
     * 封装可选的批次创建参数，避免方法参数过多
     */
    @Data
    public static class BatchCreationContext {
        // Backup/Restore 字段
        private String basePath;
        private String sftpServerName;
        
        // 公共字段
        private String remark;
        private Boolean debug;
        private Long scheduledTime;  // 定时执行时间戳（毫秒）
        
        // Upgrade 特有字段
        private String filePath;
        private String targetVersion;
        private Batch.ExecutionMode executionMode;
        private Boolean enableDownload;
        private Boolean enableBackup;
        private Boolean enableUpgrade;
        private String backupBasePath;
    }
}

