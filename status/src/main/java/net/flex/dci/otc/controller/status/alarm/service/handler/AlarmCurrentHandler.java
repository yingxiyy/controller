package net.flex.dci.otc.controller.status.alarm.service.handler;

import static net.flex.dci.otc.controller.status.util.Constants.DEFAULT_ALARM_REFRESH_THREAD_POOL_SIZE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmCurrentDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RefreshDeviceAlarmOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/10 14:09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmCurrentHandler implements IAlarmHandler {

    private final AlarmCurrentDaoService alarmCurrentDaoService;

    private final AdapterDao adapterDao;

    private final AdapterRpc adapterRpc;

    public Page<AlarmRecord> listAlarmsByConditionPaged(int offset,
            int limit,
            AlarmConditionDto alarmConditionDto) {

        log.debug("list current alarm by condition {}", alarmConditionDto);

        Page<AlarmRecord> pagedResult = alarmCurrentDaoService.listAlarmPagedByCondition(offset,
                limit, alarmConditionDto);
        return pagedResult;
    }

    @Override
    public Object findAlarm(Long alarmIndex) {
        log.debug("get alarm detail,index is {} ", alarmIndex);
        AlarmRecord alarmRecord = alarmCurrentDaoService.findByIndex(alarmIndex);
        return alarmRecord;
    }

    @Override
    public void refreshDeviceAlarm() {
        long batchStartTimestamp = System.currentTimeMillis();
        String batchId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[ALARM-REFRESH-AUDIT]Batch started. BatchID: {}, StartTime: {}", batchId,
                LocalDateTime.now());
        List<Adapter> adapterList = adapterDao.getAdapters();
        log.info("[ALARM-REFRESH-AUDIT]Batch id:{} total {} adapters need refresh the device alarm",
                batchId,
                adapterList.size());
        if (adapterList.isEmpty()) {
            log.info("[ALARM-REFRESH-AUDIT]Batch id:{} No adapters to refresh", batchId);
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(DEFAULT_ALARM_REFRESH_THREAD_POOL_SIZE, adapterList.size()));
        try {
            List<CompletableFuture<AdapterAlarmRefreshResult>> futures = adapterList.stream()
                    .map(adapter -> CompletableFuture.supplyAsync(
                            () -> refreshSingleAdapterWithAudit(adapter, batchId), executorService))
                    .collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            allFutures.get(5, TimeUnit.MINUTES);
            List<AdapterAlarmRefreshResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            generateSummaryReport(batchId, results);
        } catch (Exception ex) {
            log.error("[ALARM-REFRESH-AUDIT] BatchID: {}, Task execution failed", batchId, ex);
        } finally {
            executorService.shutdown();
            long batchEndTime = System.currentTimeMillis();
            long duration = batchEndTime - batchStartTimestamp;
            log.info(
                    "[ALARM-REFRESH-AUDIT] Batch completed. BatchID: {}, Total duration: {}ms, EndTime: {}",
                    batchId, duration, LocalDateTime.now());
        }

    }

    private void generateSummaryReport(String batchId, List<AdapterAlarmRefreshResult> results) {
        long successCount = results.stream()
                .filter(AdapterAlarmRefreshResult::isSuccess)
                .count();
        long failCount = results.size() - successCount;
        double avgDuration = results.stream()
                .mapToLong(AdapterAlarmRefreshResult::getDuration)
                .average()
                .orElse(0.0);

        List<String> failedAdapters = results.stream()
                .filter(r -> !r.isSuccess())
                .map(r -> r.getAdapterName() + ": " + r.getMessage())
                .collect(Collectors.toList());

        log.info(
                "[ALARM-REFRESH-AUDIT] BatchID: {}, Summary - Total: {}, Success: {}, Failed: {}, Avg Duration: {:.2f}ms",
                batchId, results.size(), successCount, failCount, avgDuration);

        if (!failedAdapters.isEmpty()) {
            log.warn("[ALARM-REFRESH-AUDIT] BatchID: {}, Failed adapters: {}",
                    batchId, String.join("; ", failedAdapters));
        }
    }


    private AdapterAlarmRefreshResult refreshSingleAdapterWithAudit(Adapter adapter,
            String batchId) {
        long startTimestamp = System.currentTimeMillis();
        String logPrefix = String.format("[ADAPTER-REFRESH] BatchID: %s, AdapterIp: %s, Name: %s",
                batchId, adapter.getIp(), adapter.getName());
        log.debug("{}-Refresh start", logPrefix);
        AdapterAlarmRefreshResult refreshResult = new AdapterAlarmRefreshResult();
        refreshResult.setAdapterId(adapter.getName().getValue());
        refreshResult.setAdapterName(adapter.getName().getValue());
        refreshResult.setStartTime(startTimestamp);
        try {
            RefreshDeviceAlarmOutput output = adapterRpc.refreshDeviceAlarm(
                    adapter);
            long adapterEndTime = System.currentTimeMillis();
            long duration = adapterEndTime - startTimestamp;
            refreshResult.setDuration(duration);
            refreshResult.setResultType(output.getReturnCode());
            if (output.getReturnCode() == RpcResultType.Success) {
                refreshResult.setSuccess(true);
                refreshResult.setMessage(output.getReturnMessage());
            } else {
                refreshResult.setSuccess(false);
                refreshResult.setError(output.getReturnMessage());
            }
        } catch (Exception ex) {
            long adapterEndTime = System.currentTimeMillis();
            long duration = adapterEndTime - startTimestamp;
            refreshResult.setDuration(duration);
            refreshResult.setSuccess(false);
            refreshResult.setDuration(duration);
            refreshResult.setMessage("Refresh failed: " + ex.getMessage());
            refreshResult.setError(ex.getClass().getSimpleName());
            log.error("{} - Refresh failed, Duration: {}ms", logPrefix, duration, ex);
        }
        return refreshResult;
    }

    @Data
    private static class AdapterAlarmRefreshResult {

        private String adapterId;
        private String adapterName;
        private boolean success;
        private String message;
        private Long duration;
        private Long startTime;
        private RpcResultType resultType;
        private String error;
    }
}
