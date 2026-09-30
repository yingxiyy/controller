package net.flex.dci.otn.controller.taskinfo.core.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.taskinfo.utils.ConvertorUtils;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskInfoBatchBuffer {

    private final Queue<WriteRequest> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    private final TaskInfoDaoService taskInfoDaoService;
    private final TaskInfoMessager taskInfoMessager;

    static final int MAX_BATCH_SIZE = 200;
    static final long FLUSH_INTERVAL_MS = 100;
    private volatile boolean running = true;

    public TaskInfoBatchBuffer(TaskInfoDaoService taskInfoDaoService,
            TaskInfoMessager taskInfoMessager) {
        this.taskInfoDaoService = taskInfoDaoService;
        this.taskInfoMessager = taskInfoMessager;
        startFlushThread();
    }

    public void submit(TaskInfo entity, TaskInfoMessage msg) {
        buffer.offer(new WriteRequest(entity, msg));
    }

    public void destroy() {
        running = false;
        flush();
    }

    private void startFlushThread() {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(FLUSH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                flush();
            }
        }, "task-info-buffer-flusher");
        t.setDaemon(true);
        t.start();
    }

    void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        if (!flushing.compareAndSet(false, true)) {
            return;
        }

        try {
            List<WriteRequest> batch = new ArrayList<>(MAX_BATCH_SIZE);
            WriteRequest req;
            while ((req = buffer.poll()) != null && batch.size() < MAX_BATCH_SIZE) {
                batch.add(req);
            }
            if (batch.isEmpty()) {
                return;
            }

            long t0 = System.nanoTime();

            // 分组
            Map<String, List<WriteRequest>> grouped = new LinkedHashMap<>();
            for (WriteRequest r : batch) {
                String key = r.entity.getResourceId() + "|" + r.entity.getActionTime();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }

            long selectT0 = System.nanoTime();
            Map<String, TaskInfo> existingMap = new HashMap<>();
            List<TaskInfo> taskKeyInfos = batch.stream().map(WriteRequest::getEntity)
                    .map(task -> TaskInfo.builder().actionTime(task.getActionTime())
                            .resourceId(task.getResourceId()).build())
                    .collect(
                            Collectors.toList());
            List<TaskInfo> taskInfos = taskInfoDaoService.batchQueryByResourceAndAction(
                    taskKeyInfos);
            for (TaskInfo exist : taskInfos) {
                existingMap.put(exist.getResourceId() + "|" + exist.getActionTime(),
                        exist);
            }
//            for (WriteRequest r : batch) {
//                TaskInfo existing = taskInfoDaoService
//                        .getByResourceIdAndActionTime(r.entity.getResourceId(),
//                                r.entity.getActionTime());
//                if (existing != null) {
//                    r.entity.setId(existing.getId());
//                    existingMap.put(r.entity.getResourceId() + "|" + r.entity.getActionTime(),
//                            existing);
//                }
//            }
            long selectCostMs = (System.nanoTime() - selectT0) / 1_000_000;

            long writeT0 = System.nanoTime();
            Map<String, TaskInfoMessage> lastMsgMap = new HashMap<>();
            List<TaskInfo> toSave = new ArrayList<>();
            for (Map.Entry<String, List<WriteRequest>> entry : grouped.entrySet()) {
                String groupKey = entry.getKey();
                List<WriteRequest> group = entry.getValue();

                WriteRequest first = group.get(0);
                TaskInfo mergedEntity = first.entity;
                for (int i = 1; i < group.size(); i++) {
                    ConvertorUtils.updateTaskInfoFromMessage(mergedEntity, group.get(i).msg);
                }

                TaskInfo existing = existingMap.get(groupKey);
                if (existing != null) {
                    mergedEntity.setId(existing.getId());
                }
                toSave.add(mergedEntity);                                    // ← 先收集
                lastMsgMap.put(groupKey, group.get(group.size() - 1).msg);
            }
            List<TaskInfo> saved = taskInfoDaoService.save(toSave);
            int createCount = 0, updateCount = 0;
            for (int i = 0; i < saved.size(); i++) {
                String groupKey = saved.get(i).getResourceId() + "|" + saved.get(i).getActionTime();
                TaskInfoMessage lastMsg = lastMsgMap.get(groupKey);
                boolean isNew = existingMap.get(groupKey) == null;

                if (isNew) {
                    createCount++;
                    taskInfoMessager.notifyTaskInfoCreate(
                            ConvertorUtils.convertTaskInfoEntity2Dto(saved.get(i), lastMsg));
                } else {
                    updateCount++;
                    taskInfoMessager.notifyTaskInfoUpdate(
                            ConvertorUtils.convertTaskInfoEntity2Dto(saved.get(i), lastMsg));
                }
                if (lastMsg != null && lastMsg.hasDetail()) {
                    taskInfoDaoService.saveDetail(saved.get(i).getId(), lastMsg.getDetail());
                }
            }

//                if (existing != null) {
//                    mergedEntity.setId(existing.getId());
//                    TaskInfo saved = taskInfoDaoService.save(mergedEntity);
//                    taskInfoMessager.notifyTaskInfoUpdate(
//                            ConvertorUtils.convertTaskInfoEntity2Dto(saved, lastMsg));
//                    if (lastMsg.hasDetail()) {
//                        taskInfoDaoService.saveDetail(saved.getId(), lastMsg.getDetail());
//                    }
//                } else {
//                    TaskInfo saved = taskInfoDaoService.save(mergedEntity);
//                    taskInfoMessager.notifyTaskInfoCreate(
//                            ConvertorUtils.convertTaskInfoEntity2Dto(saved, lastMsg));
//                    if (lastMsg.hasDetail()) {
//                        taskInfoDaoService.saveDetail(saved.getId(), lastMsg.getDetail());
//                    }
//                }
//            }
            long writeCostMs = (System.nanoTime() - writeT0) / 1_000_000;
            long totalCostMs = (System.nanoTime() - t0) / 1_000_000;

            log.warn(
                    "batch flush {} groups ({}c {}u) from {} msgs total={}ms select={}ms write={}ms",
                    grouped.size(), createCount, updateCount, batch.size(),
                    totalCostMs, selectCostMs, writeCostMs);

        } catch (Exception e) {
            log.error("batch flush failed, will retry", e);
        } finally {
            flushing.set(false);
        }
    }

    @Data
    @AllArgsConstructor
    private static class WriteRequest {

        private TaskInfo entity;
        private TaskInfoMessage msg;
    }
}
