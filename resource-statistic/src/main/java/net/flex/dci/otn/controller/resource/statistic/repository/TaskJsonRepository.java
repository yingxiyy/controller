package net.flex.dci.otn.controller.resource.statistic.repository;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.TASK_FILE_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskDto;
import net.flex.dci.otn.controller.resource.statistic.properties.ExportFileProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2026/6/20
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskJsonRepository {

    private final ExportFileProperties exportFileProperties;

    private File jsonFile;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
            new JavaTimeModule());


    private Map<String, ExportTaskDto> taskCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {

        Path dir = Paths.get(exportFileProperties.getPath());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("failed to create export dir: {}", dir, e);
        }
        this.jsonFile = dir.resolve(TASK_FILE_NAME).toFile();

        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                ExportTaskDto[] tasks = objectMapper.readValue(jsonFile, ExportTaskDto[].class);
                for (ExportTaskDto task : tasks) {
                    taskCache.put(task.getTaskId(), task);
                }
                log.info("loaded {} tasks from {}", taskCache.size(), jsonFile);
            } catch (IOException e) {
                log.error("failed to load tasks from {}", jsonFile, e);
            }
        } else {
            log.info("no existing task file, starting fresh");
        }

        // 启动时清理超过 TTL 的过期任务
        evictExpired();
    }

    /**
     * 清理超过 TTL 天的过期任务记录
     */
    private void evictExpired() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(exportFileProperties.getTtl());
        List<String> expiredIds = taskCache.values().stream()
                .filter(t -> t.getCreateTime() != null && t.getCreateTime().isBefore(deadline))
                .map(ExportTaskDto::getTaskId)
                .collect(Collectors.toList());

        if (expiredIds.isEmpty()) {
            return;
        }

        expiredIds.forEach(taskCache::remove);
        log.info("evicted {} expired tasks (ttl={}d)", expiredIds.size(),
                exportFileProperties.getTtl());
        flushToDisk();
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void scheduledEvictExpired() {
        log.info("scheduled task eviction started");
        evictExpired();
    }

    public synchronized void save(ExportTaskDto task) {
        taskCache.put(task.getTaskId(), task);
        flushToDisk();
    }

    public ExportTaskDto findById(String taskId) {
        return taskCache.get(taskId);
    }

    public List<ExportTaskDto> findAll() {
        return new ArrayList<>(taskCache.values());
    }

    public synchronized void delete(String taskId) {
        taskCache.remove(taskId);
        flushToDisk();
    }

    private void flushToDisk() {
        try {
            objectMapper.writeValue(jsonFile, taskCache.values());
        } catch (IOException e) {
            log.error("flush to disk failed", e);
        }
    }
}
