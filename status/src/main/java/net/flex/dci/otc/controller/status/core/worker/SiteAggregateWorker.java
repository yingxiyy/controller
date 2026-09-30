package net.flex.dci.otc.controller.status.core.worker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.RackAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.SiteNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.ViewNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.changer.StateChanger;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.RackAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteNodeAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 2026/9/27
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteAggregateWorker {

    @Qualifier("siteAggregateExecutor")
    private final ExecutorService siteAggregateExecutor;

    private final ViewNodeAlarmStateCalculator viewNodeAlarmStateCalculator;

    private final RackAlarmStateCalculator rackAlarmStateCalculator;

    private final SiteNodeAlarmStateCalculator siteNodeAlarmStateCalculator;

    private final AtomicLong submitSeq = new AtomicLong();
    private final AtomicLong executedCounter = new AtomicLong();
    private final AtomicLong droppedCounter = new AtomicLong();

    private final ConcurrentMap<String, Long> pendingNeIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> inFlight = new ConcurrentHashMap<>();
    private static final long INFLIGHT_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(30_000);

    private long lastSubmit;

    private long lastExecuted;
    private int round;

    private final StateChanger stateChanger;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("site-aggregate-schedule-%d").build());

    @PostConstruct
    private void start() {
        scheduler.scheduleAtFixedRate(this::drain, 200, 200, TimeUnit.MILLISECONDS);
    }


    public void submit(String neId) {
        log.info("submit the neId:{}", neId);
        pendingNeIds.put(neId, submitSeq.incrementAndGet());
    }

    private void drain() {
        try {
            if (++round >= 50) {
                round = 0;
                long s = submitSeq.get();
                long e = executedCounter.get();
                long dSubmit = s - lastSubmit;
                long dExec = e - lastExecuted;
                log.info("[site-aggregate] submit={} executed={} fold={}% pending={} inFlight={}",
                        dSubmit, dExec,
                        dSubmit == 0 ? 0 : 100 - dExec * 100 / dSubmit,
                        pendingNeIds.size(), inFlight.size());
                lastSubmit = s;
                lastExecuted = e;
            }
            long nanoTime = System.nanoTime();
            inFlight.entrySet().removeIf(e -> {
                if (nanoTime - e.getValue() > INFLIGHT_TIMEOUT_NANOS) {
                    log.warn("[site-aggregate] site={} inFlight timeout, force release",
                            e.getKey());
                    return true;
                }
                return false;
            });
            if (pendingNeIds.isEmpty()) {
                return;
            }
            Set<String> neIds = new HashSet<>();
            for (Map.Entry<String, Long> entry : pendingNeIds.entrySet()) {
                if (pendingNeIds.remove(entry.getKey(), entry.getValue())) {
                    neIds.add(entry.getKey());
                }
            }
            if (neIds.isEmpty()) {
                return;
            }
            Map<String, Set<String>> bySite = neIds.stream().collect(Collectors.groupingBy(
                    PhysicalNodeIdNamingRule::getSiteId, Collectors.toSet()));
            for (Map.Entry<String, Set<String>> entry : bySite.entrySet()) {
                String siteId = entry.getKey();
                Set<String> refNeIds = entry.getValue();
                long token = System.nanoTime();
                if (inFlight.putIfAbsent(siteId, token) != null) {
                    refNeIds.forEach(neId -> pendingNeIds.put(neId, submitSeq.incrementAndGet()));
                    continue;
                }
                try {
                    siteAggregateExecutor.execute(() -> {
                        try {
                            if (!Long.valueOf(token).equals(inFlight.get(siteId))) {
                                return;
                            }
                            updateRefSiteNodeAlarmState(siteId, refNeIds);
                            executedCounter.addAndGet(refNeIds.size());
                        } catch (Throwable t) {
                            log.error("[site-aggregate] site={} failed", siteId, t);
                            refNeIds.forEach(
                                    id -> pendingNeIds.put(id, submitSeq.incrementAndGet()));
                        } finally {
                            inFlight.remove(siteId, token);
                        }
                    });
                } catch (RejectedExecutionException executionException) {
                    inFlight.remove(siteId, token);
                    refNeIds.forEach(neId -> pendingNeIds.put(neId, submitSeq.incrementAndGet()));
                    long dropped = droppedCounter.incrementAndGet();
                    if (dropped % 100 == 1) {
                        log.warn("[site-aggregate] executor full,request neIds={} dropped count={}",
                                neIds.size(), dropped);
                    }
                }

            }

        } catch (Throwable t) {
            log.error("[site-aggregate] drain failed, scheduler keeps alive", t);
        }
    }

    /**
     * update ref site node alarm state like rack site view node
     *
     * @param siteId site id
     * @param neIds under site ne ids
     */
    private void updateRefSiteNodeAlarmState(String siteId, Set<String> neIds) {
        log.debug("start to calculate the site :{} and ne:{} ref alarm state", siteId, neIds);
        List<RackAlarmState> rackAlarmStates = rackAlarmStateCalculator.calculateSiteRefRackCurrentStateBySiteId(
                siteId);
        List<ViewNodeAlarmState> viewNodeAlarmStates = viewNodeAlarmStateCalculator.calculateViewNodeAlarmsByNeIds(
                neIds);
        SiteNodeAlarmState siteNodeAlarmState = siteNodeAlarmStateCalculator.calculateSiteCurrentState(
                siteId);
        updateRefNodeAlarmState(rackAlarmStates, viewNodeAlarmStates, siteNodeAlarmState);
    }

    private void updateRefNodeAlarmState(List<RackAlarmState> rackAlarmStates,
            List<ViewNodeAlarmState> viewNodeAlarmStates, SiteNodeAlarmState siteNodeAlarmState) {
        log.debug("update the ref view node and site node alarm state");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .rackAlarmStateList(rackAlarmStates)
                .viewNodeAlarmStates(viewNodeAlarmStates).siteNodeAlarmState(siteNodeAlarmState)
                .build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build());
    }

    @PreDestroy
    public void stop() {
        drain();
        scheduler.shutdown();
        siteAggregateExecutor.shutdown();
        try {
            if (!siteAggregateExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                siteAggregateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
