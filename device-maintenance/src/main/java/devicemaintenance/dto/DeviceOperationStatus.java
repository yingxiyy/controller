package devicemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 设备操作状态响应DTO
 * 包含MongoDB op-phy-node集合中所有维护操作相关字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceOperationStatus {
    
    // 设备基本信息
    private String deviceId;              // node-id
    private String deviceName;            // friendly-name
    private String communicationStatus;   // communication-status
    private String operationalState;      // operational-state
    private String currentSoftware;       // current-software
    private String currentDatabase;       // current-database
    private String systemDateTime;        // system.current-datetime
    
    // 软件操作状态
    private SoftwareOperations softwareOperations;
    
    // 数据库操作状态
    private DatabaseOperations databaseOperations;
    
    /**
     * 软件相关操作状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class SoftwareOperations {
        
        // 下载操作状态
        private SoftwareDownload download;
        
        // 升级操作状态
        private SoftwareUpgrade upgrade;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public static class SoftwareDownload {
            private String state;               // download.download-state
            private String fileName;            // download.file-name
            private String softwareVersion;     // download.software-version
            private String downloadTime;        // download.download-time
            private String expectedDuration;    // download.expected-duration
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public static class SoftwareUpgrade {
            private String state;               // upgrade.upgrade-state
            private String upgradeTime;         // upgrade.upgrade-time
            private String rollbackFile;       // upgrade.rollback-file
            private String rollbackSoftware;   // upgrade.rollback-software
        }
    }
    
    /**
     * 数据库相关操作状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class DatabaseOperations {
        
        // 备份操作状态
        private DatabaseBackup backup;
        
        // 恢复操作状态
        private DatabaseRestore restore;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public static class DatabaseBackup {
            private String state;               // backup.backup-state
            private String backupFile;          // backup.backup-file
            private String backupTime;          // backup.backup-time
            private String backupDatabase;      // backup.backup-database
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public static class DatabaseRestore {
            private String state;               // restore.restore-state
            private String restoreTime;         // restore.restore-time
            private String restoreFromFile;     // restore.backup-file
            private String restoreDatabase;     // restore.restore-database
        }
    }
}
