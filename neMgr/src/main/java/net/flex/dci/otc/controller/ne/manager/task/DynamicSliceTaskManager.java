package net.flex.dci.otc.controller.ne.manager.task;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.MODULE;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.dto.NeRegisteredInfo;
import net.flex.dci.otc.controller.ne.manager.monitor.settings.MonitorSetting;
import net.flex.dci.otc.controller.ne.manager.monitor.state.NeStateMonitor;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otc.zkclient4boot.util.ConfLoader;
import org.apache.curator.shaded.com.google.common.hash.Hashing;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 2026/5/19
 *
 * @author musa
 * @version 1.0
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicSliceTaskManager {

    private final NeStateMonitor neStateMonitor;

    private final MonitorSetting monitorSetting;

    private final PhyNodeDao phyNodeDao;

    private final AdapterBalancer adapterBalancer;

    private static final int MIN_SLICE_COUNT = 1;
    private static final int MAX_SLICE_COUNT = 30;
    private static final double CPU_HIGH_THRESHOLD = 0.75;
    private static final double CPU_LOW_THRESHOLD = 0.25;
    private static final long AUTO_ADJUST_INTERVAL = 600;
    private static final long ADJUST_COOLDOWN = 1800;
    private static final int CONSECUTIVE_HIGH_COUNT = 3;
    private static final long DEFAULT_STAGGER_SECONDS = 10;
    private static final long MAX_STAGGER_SECONDS = 30;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    private volatile int currentSliceCount;
    private volatile boolean autoAdjustEnabled = true;

    private volatile boolean isRunning = false;

    private final AtomicBoolean adjustLock = new AtomicBoolean(false);
    private final AtomicInteger consecutiveHighLoad = new AtomicInteger(0);
    private volatile long lastAdjustTime = 0;
    private volatile long lastCycleStartTime = 0;

    private String myInstanceId = ConfLoader.buildDetails().getId();

    @PostConstruct
    public void init() {
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ne-monitor-");
        executor.initialize();
        currentSliceCount = getValidSliceCount(monitorSetting.getMonitorSettings().getSliceCount());
        log.info(
                "initializing monitor task ，init slice count：{}，auto adjust enabled：{}，stagger interval：{}s",
                currentSliceCount, autoAdjustEnabled, getStaggerSeconds());
    }

    public void initTasks() {
        scheduler.schedule(this::startInspectionCycle, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::autoAdjustCpuLoad, AUTO_ADJUST_INTERVAL,
                AUTO_ADJUST_INTERVAL, TimeUnit.SECONDS);
        log.info(
                "Inspection scheduler started. First cycle will start in 60 seconds, inspection interval: 5 minutes");
    }

    private void autoAdjustCpuLoad() {
        if (!autoAdjustEnabled) {
            return;
        }
        if (System.currentTimeMillis() - lastAdjustTime < ADJUST_COOLDOWN) {
            log.debug("during the cool down interval,skip auto adjust");
            return;
        }
        double cpuLoad = getSystemCpuLoad();
        log.info("current cpu load {:.1f}% current slice count:{}", cpuLoad * 100,
                currentSliceCount);
        if (cpuLoad > CPU_HIGH_THRESHOLD) {
            int count = consecutiveHighLoad.incrementAndGet();
            log.info("Detected high load, detect times: {}/{}", currentSliceCount,
                    CONSECUTIVE_HIGH_COUNT);
            if (count >= CONSECUTIVE_HIGH_COUNT && currentSliceCount < MAX_SLICE_COUNT) {
                int newSliceCount = Math.min(currentSliceCount + 3, MAX_SLICE_COUNT);
                log.info("detected {} high load times ，expand slice count：{} -> {}",
                        CONSECUTIVE_HIGH_COUNT, currentSliceCount, newSliceCount);
                adjustSliceCount(newSliceCount);
            }
            return;

        }
        if (cpuLoad < CPU_LOW_THRESHOLD && currentSliceCount > MIN_SLICE_COUNT) {
            consecutiveHighLoad.set(0);
            int newSliceCount = Math.max(currentSliceCount - 2, MIN_SLICE_COUNT);
            log.info("Detected low load, reduce slice count: {} -> {}", currentSliceCount,
                    newSliceCount);
            adjustSliceCount(newSliceCount);
            return;
        }
        consecutiveHighLoad.set(0);
    }

    private void adjustSliceCount(int newSliceCount) {
        if (!adjustLock.compareAndSet(false, true)) {
            log.warn("already have the job execute,skip current adjust task");
            return;
        }
        try {
            int validCount = getValidSliceCount(newSliceCount);
            if (validCount == currentSliceCount) {
                return;
            }
            log.info(
                    "adjust current slice count:{}->{} will take effect in the next inspection cycle",
                    currentSliceCount, newSliceCount);
            currentSliceCount = validCount;
            lastAdjustTime = System.currentTimeMillis();
            consecutiveHighLoad.set(0);
        } finally {
            adjustLock.set(false);
        }
    }

    private double getSystemCpuLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
        }
        return osBean.getSystemLoadAverage() / Runtime.getRuntime().availableProcessors();
    }

    private void startInspectionCycle() {
        if (isRunning) {
            log.warn("current slice job interval is not over,add more slice count");
            return;
        }
        isRunning = true;
        lastCycleStartTime = System.currentTimeMillis();
        log.info("========================================");
        log.info("start to new monitor task");
        log.info("current slice count:{} and stagger time:{} s", currentSliceCount,
                getStaggerSeconds());
        log.info("=========================================");

        List<String> monitoringNeIds = phyNodeDao.listAllExistIpAndMonitoringNeIds();
        ShardView shard = resolveShardView();
        monitoringNeIds = monitoringNeIds.stream()
                .filter(neId -> Math.abs(shardHash(neId) % shard.instanceCount) == shard.myIndex)
                .collect(Collectors.toList());
        if (monitoringNeIds.isEmpty()) {
            log.info("no ne assigned to this instance (index={}/{}), skip",
                    shard.myIndex, shard.instanceCount);
            isRunning = false;
            return;
        }
        NeRegisteredInfo neRegisteredInfo = adapterBalancer.getNeRegisteredInfo(monitoringNeIds);
        executeSlice(0, monitoringNeIds, neRegisteredInfo);
    }


    private void executeSlice(int sliceIndex, List<String> monitoringNeIds,
            NeRegisteredInfo neRegisteredInfo) {
        if (sliceIndex >= currentSliceCount) {
            long cycleCost = System.currentTimeMillis() - lastCycleStartTime;
            log.info("========================================");
            log.info("execute slice finished");
            log.info("total cost：{}min{}second", cycleCost / 60000, (cycleCost % 60000) / 1000);
            log.info("execute slice count：{}", currentSliceCount);
            log.info("========================================");

            long interval = monitorSetting.getMonitorSettings().getMonitorInterval().toMillis();
            long nextDelay = Math.max(0, interval - cycleCost);
            log.info("Next cycle will start in {} minutes and {} seconds", nextDelay / 60000,
                    (nextDelay % 60000) / 1000);

            scheduler.schedule(this::startInspectionCycle, nextDelay, TimeUnit.MILLISECONDS);
            isRunning = false;
            return;
        }

        Set<String> sliceNeIdSet = monitoringNeIds.stream()
                .filter(neId -> Math.abs(neId.hashCode() % currentSliceCount) == sliceIndex)
                .collect(Collectors.toSet());
        if (sliceNeIdSet.isEmpty()) {
            log.warn("slice [{}] have no ne, skip", sliceIndex);
            scheduler.schedule(
                    () -> executeSlice(sliceIndex + 1, monitoringNeIds, neRegisteredInfo),
                    getStaggerSeconds() * 1000, TimeUnit.MILLISECONDS);
            return;
        }
        NeRegisteredInfo sliceNeRegisteredInfo = NeRegisteredInfo.builder()
                .registeredNes(NeManagerUtils.getIntersectionSetByGuava(
                        neRegisteredInfo.getRegisteredNes(), sliceNeIdSet))
                .notRegisteredNes(NeManagerUtils.getIntersectionSetByGuava(
                        neRegisteredInfo.getNotRegisteredNes(), sliceNeIdSet))
                .needSynchronizedNes(NeManagerUtils.getIntersectionSetByGuava(
                        neRegisteredInfo.getNeedSynchronizedNes(), sliceNeIdSet))
                .build();

        NeRegisteredInfo finalSliceInfo = sliceNeRegisteredInfo;
        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.info("start to execute slice [{}/{}]", sliceIndex + 1, currentSliceCount);
                neStateMonitor.checkAndSynchronizeStateBySlice(sliceIndex, currentSliceCount,
                        finalSliceInfo);
                long cost = System.currentTimeMillis() - startTime;
                log.info("slice [{}/{}] finished,cost:{} ms", sliceIndex, currentSliceCount, cost);
                if (cost > 180000) {
                    log.warn(
                            "slice [{}/{}] execute duration to long({}s)，advise add more slice count",
                            sliceIndex + 1, currentSliceCount, cost / 1000);
                }
            } catch (Exception e) {
                log.error("slice [{}/{}] execute failed", sliceIndex + 1, currentSliceCount, e);
            } finally {
                long stagger = getStaggerSeconds() * 1000;
                log.debug("wait {} seconds to execute the next slice", stagger / 1000);
                scheduler.schedule(
                        () -> executeSlice(sliceIndex + 1, monitoringNeIds, neRegisteredInfo),
                        stagger, TimeUnit.MILLISECONDS);
            }
        });
    }

    private int getValidSliceCount(int count) {
        return Math.max(MIN_SLICE_COUNT, Math.min(count, MAX_SLICE_COUNT));
    }

    private long getStaggerSeconds() {
        Long configStagger = monitorSetting.getMonitorSettings().getStaggerSeconds();
        if (configStagger == null || configStagger <= 0) {
            return DEFAULT_STAGGER_SECONDS;
        }
        return Math.min(configStagger, MAX_STAGGER_SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        log.info("application shut down,stop all the inspection task");
        scheduler.shutdown();
        executor.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            java.util.concurrent.ThreadPoolExecutor nativeExecutor = executor.getThreadPoolExecutor();
            if (!nativeExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                nativeExecutor.shutdownNow();
                log.warn("Task executor did not terminate gracefully, forced shutdown");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executor.getThreadPoolExecutor().shutdownNow();
            log.error("Shutdown interrupted, forced to stop all tasks immediately", e);
            Thread.currentThread().isInterrupted();
        }
        log.info("all the inspection shut down");
    }


    private ShardView resolveShardView() {
        ShardView shardView = new ShardView();
        try {
            List<InstanceDetails> all = DciInstancesUtils.getAllStateInstancesByModule(MODULE);
            all.sort(Comparator.comparing(InstanceDetails::getId));
            shardView.instanceCount = all.size();
            shardView.myIndex = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(myInstanceId)) {
                    shardView.myIndex = i;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("resolve shard view failed, fallback to single-instance", e);
        }
        if (shardView.myIndex < 0) {
            shardView.instanceCount = 1;
            shardView.myIndex = 0;
        }
        return shardView;
    }

    private int shardHash(String neId) {
        return Hashing.murmur3_32().hashString(neId, StandardCharsets.UTF_8).asInt();
    }

    private static class ShardView {

        int instanceCount;
        int myIndex;
    }
}
