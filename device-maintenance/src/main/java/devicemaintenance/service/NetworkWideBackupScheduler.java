package devicemaintenance.service;

import devicemaintenance.dto.DeviceBackupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

/**
 * 全网设备自动备份调度器
 * 
 * 功能：
 * - 定时自动备份全网所有设备的配置
 * - 直接复用 DeviceBackupService.batchBackupDevices() 方法
 * - 通过配置文件控制开关和执行时间
 * - TaskInfo通知、operator、endTime处理方式与 batch backup 完全一致
 * - 备份完成后自动清理过期备份（可配置）
 * 
 * 配置项（application.yml）：
 * 
 * device-maintenance:
 *   network-backup:
 *     enabled: true                          # 是否启用全网备份（默认false）
 *     cron: "0 0 2 * * ?"                    # cron表达式：每天凌晨2点
 *     sftp-server: "backup-sftp-server"     # SFTP服务器名称
 *     base-path: "/dbbackup/network-wide"   # 备份根路径
 *     cleanup:
 *       enabled: true                        # 是否启用过期清理
 *       retention-days: 7                    # 备份保留天数
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "device-maintenance.network-backup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class NetworkWideBackupScheduler {

    private final PhyNodeDao phyNodeDao;
    private final DeviceBackupService deviceBackupService;
    private final BackupCleanupService backupCleanupService;

    @PostConstruct
    public void init() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🌐 [全网备份] NetworkWideBackupScheduler 已初始化");
        log.info("  定时任务cron: ${device-maintenance.network-backup.cron}");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Value("${device-maintenance.network-backup.sftp-server}")
    private String sftpServerName;

    @Value("${device-maintenance.network-backup.base-path}")
    private String basePath;

    @Value("${device-maintenance.network-backup.batch-size:20}")
    private int batchSize;

    @Value("${device-maintenance.network-backup.batch-interval-seconds:30}")
    private int batchIntervalSeconds;

    // 注意：
    // - 线程池已控制并发（max-pool-size: 20）
    // - cron 执行间隔为24小时，不会重叠
    // - 单个设备失败不影响其他设备
    // - 分批下发：每批 batchSize 个设备，批次间隔 batchIntervalSeconds 秒

    /**
     * 全网设备自动备份定时任务
     *
     * 执行时间由配置文件的cron表达式控制
     * 例如：0 0 2 * * ? = 每天凌晨2点
     *
     * 分批下发：
     * - 每批下发 batchSize 个设备（默认20）
     * - 批次间隔 batchIntervalSeconds 秒（默认30秒）
     * - 不等待上一批完成，只是控制下发节奏
     *
     * 注意：
     * - 线程池控制并发（max-pool-size: 20）
     * - 单个设备失败不影响其他设备（CompletableFuture 异常隔离）
     * - 直接复用 batchBackupDevices()，TaskInfo 通知自动发送
     */
    @Scheduled(cron = "${device-maintenance.network-backup.cron}")
    public void executeNetworkWideBackup() {
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        log.info("================================================================================");
        log.info("🌐 全网设备自动备份任务开始");
        log.info("  开始时间: {}", startTime);
        log.info("  SFTP服务器: {}", sftpServerName);
        log.info("  备份路径: {}", basePath);
        log.info("  分批配置: 每批 {} 个设备，间隔 {} 秒", batchSize, batchIntervalSeconds);
        log.info("================================================================================");

        try {
            // 1. 调用 phyNodeDao 获取全网设备ID列表
            List<String> allDeviceIds = getAllDeviceIds();

            if (allDeviceIds.isEmpty()) {
                log.warn("⚠️  未找到任何设备，跳过全网备份");
                return;
            }

            log.info("📊 全网设备数量：{} 个", allDeviceIds.size());

            // 2. 分批下发
            int totalDevices = allDeviceIds.size();
            int effectiveBatchSize = (batchSize <= 0) ? totalDevices : batchSize;
            int totalBatches = (int) Math.ceil((double) totalDevices / effectiveBatchSize);

            log.info("📦 分批计划：共 {} 批，每批最多 {} 个设备", totalBatches, effectiveBatchSize);

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int fromIndex = batchIndex * effectiveBatchSize;
                int toIndex = Math.min(fromIndex + effectiveBatchSize, totalDevices);
                List<String> batchDeviceIds = allDeviceIds.subList(fromIndex, toIndex);

                int currentBatch = batchIndex + 1;
                log.info("────────────────────────────────────────────────────────────────────────────────");
                log.info("📤 下发第 {}/{} 批，设备数量: {} (索引 {}-{})",
                    currentBatch, totalBatches, batchDeviceIds.size(), fromIndex, toIndex - 1);

                // 构造批量备份请求
                DeviceBackupDto.BatchBackupRequest request = new DeviceBackupDto.BatchBackupRequest();
                request.setBatchName("cron_all_devices_backup_" + dateStr + "_batch" + currentBatch);
                request.setDeviceIds(batchDeviceIds);
                request.setSftpServerName(sftpServerName);
                request.setBasePath(basePath);
                request.setScheduledTime(null);  // 立即执行
                request.setRemark("系统定时任务-全网备份-第" + currentBatch + "批");

                // 调用批量备份方法（异步执行，不阻塞）
                try {
                    DeviceBackupDto.BatchBackupSummary summary = deviceBackupService.batchBackupDevices(request);
                    log.info("  ✅ 第 {} 批已提交: batchId={}, 状态={}",
                        currentBatch, summary.getBatchId(), summary.getStatus());
                } catch (Exception batchEx) {
                    log.error("  ❌ 第 {} 批提交失败: {}", currentBatch, batchEx.getMessage());
                    // 继续下一批，不中断整个流程
                }

                // 如果不是最后一批，等待间隔时间
                if (batchIndex < totalBatches - 1 && batchIntervalSeconds > 0) {
                    log.info("  ⏳ 等待 {} 秒后下发下一批...", batchIntervalSeconds);
                    try {
                        Thread.sleep(batchIntervalSeconds * 1000L);
                    } catch (InterruptedException ie) {
                        log.warn("  ⚠️ 等待被中断");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            log.info("────────────────────────────────────────────────────────────────────────────────");
            log.info("✅ 全网备份任务已全部提交完成");
            log.info("  总设备数: {}", totalDevices);
            log.info("  总批次数: {}", totalBatches);

            // 3. 备份完成后，清理过期备份
            log.info("────────────────────────────────────────────────────────────────────────────────");
            try {
                BackupCleanupService.CleanupResult cleanupResult =
                    backupCleanupService.cleanupExpiredBackups(sftpServerName, basePath);

                if (cleanupResult.getFilesDeleted() > 0) {
                    log.info("🧹 过期备份清理完成: 删除 {} 个文件", cleanupResult.getFilesDeleted());
                }
            } catch (Exception cleanupEx) {
                log.warn("⚠️ 过期备份清理失败（不影响本次备份）: {}", cleanupEx.getMessage());
            }

            log.info("================================================================================");

        } catch (Exception e) {
            log.error("❌ 全网备份任务执行异常", e);
            log.info("================================================================================");
        }
    }

    /**
     * 获取全网所有设备ID列表
     * 
     * ⭐ 调用 phyNodeDao.listConfigPhyNodes() 获取所有配置态设备
     * ✅ 排除IP地址为空的设备（根据客户需求）
     * 
     * @return 设备ID列表（格式：Site-xxx#Ne-xxx）
     */
    private List<String> getAllDeviceIds() {
        log.info("📡 调用 phyNodeDao 获取全网设备列表...");

        try {
            // ⭐ 关键：调用 phyNodeDao 获取所有配置态设备（2024-12-30客户需求变更）
            List<Node> allNodes = phyNodeDao.listConfigPhyNodes();
            
            if (allNodes == null || allNodes.isEmpty()) {
                log.warn("  ⚠️  未查询到任何配置态设备");
                return new ArrayList<>();
            }

            log.info("  ✓ 配置态设备总数: {}", allNodes.size());

            // 提取设备ID，并过滤掉IP为空的设备
            int totalCount = allNodes.size();
            int filteredCount = 0;

            List<String> deviceIds = new ArrayList<>();
            for (Node node : allNodes) {
                // 获取设备ID
                String nodeId = (node.getNodeId() != null) ? node.getNodeId().getValue() : null;
                if (nodeId == null || nodeId.isEmpty()) {
                    continue;
                }

                // 检查IP地址是否存在
                String ip = getNodeIp(node);
                if (ip == null || ip.trim().isEmpty()) {
                    filteredCount++;
                    log.debug("  ⊖ 过滤掉IP为空的设备: {}", nodeId);
                    continue;
                }

                // 通过所有检查，添加到列表
                deviceIds.add(nodeId);
            }

            log.info("  ✅ 成功获取 {} 个设备（总数: {}，过滤掉IP为空的设备: {}）", 
                    deviceIds.size(), totalCount, filteredCount);

            return deviceIds;

        } catch (Exception e) {
            log.error("  ❌ 获取全网设备列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取设备IP地址
     * 
     * @param node 设备节点
     * @return IP地址，如果不存在则返回null
     */
    private String getNodeIp(Node node) {
        try {
            Node1 physicalData = node.getAugmentation(Node1.class);
            if (physicalData != null && physicalData.getPhysical() != null) {
                Physical physical = physicalData.getPhysical();
                return physical.getIp();
            }
        } catch (Exception e) {
            log.trace("  获取设备IP失败: nodeId={}, error={}", 
                    node.getNodeId() != null ? node.getNodeId().getValue() : "unknown", 
                    e.getMessage());
        }
        return null;
    }
}
