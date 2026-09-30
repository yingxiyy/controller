package devicemaintenance.service;

import devicemaintenance.entity.Batch;
import devicemaintenance.listener.SystemChangeNotificationListener;
import devicemaintenance.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 自动升级批次巡检与自愈。
 *
 * 目标：
 * - 当任务状态已经落库，但“触发下一步”的通知链路漏掉时，周期性补触发
 * - 只处理 AUTOMATIC 升级批次，不影响 MANUAL 模式
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "device-maintenance.auto-upgrade-reconcile",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AutoUpgradeBatchReconcileService {

    private static final Set<Batch.BatchStatus> ACTIVE_BATCH_STATUSES = EnumSet.of(
        Batch.BatchStatus.RUNNING,
        Batch.BatchStatus.DOWNLOADING,
        Batch.BatchStatus.BACKING_UP,
        Batch.BatchStatus.UPGRADING
    );

    private final BatchRepository batchRepository;
    private final SystemChangeNotificationListener systemChangeNotificationListener;

    @Value("${device-maintenance.auto-upgrade-reconcile.max-batches-per-scan:200}")
    private int maxBatchesPerScan;

    @Scheduled(fixedDelayString = "${device-maintenance.auto-upgrade-reconcile.fixed-delay-ms:30000}",
               initialDelayString = "${device-maintenance.auto-upgrade-reconcile.initial-delay-ms:45000}")
    public void reconcileAutomaticUpgradeBatches() {
        try {
            List<Batch> upgradeBatches = batchRepository.findByBatchType(Batch.BatchType.UPGRADE);
            long candidateCount = upgradeBatches.stream()
                .filter(this::shouldReconcile)
                .limit(maxBatchesPerScan)
                .peek(batch -> systemChangeNotificationListener.reconcileAutomaticBatchProgress(batch.getBatchId()))
                .count();

            if (candidateCount > 0) {
                log.info("自动升级批次巡检完成: reconciled={}", candidateCount);
            } else {
                log.debug("自动升级批次巡检完成: 无需处理的批次");
            }
        } catch (Exception e) {
            log.error("自动升级批次巡检异常", e);
        }
    }

    private boolean shouldReconcile(Batch batch) {
        return batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC
            && ACTIVE_BATCH_STATUSES.contains(batch.getStatus());
    }
}
