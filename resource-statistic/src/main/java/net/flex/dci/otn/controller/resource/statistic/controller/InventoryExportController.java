package net.flex.dci.otn.controller.resource.statistic.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.resource.statistic.dto.ExportResultPreviewDto;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskDto;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskService;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskStatus;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源清单导出控制器（异步版本） 客户端提交导出请求 → 获得 taskId → 轮询任务状态 → 下载文件
 *
 * @version 1.0
 * @date 2026/6/11
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/inventory/export")
public class InventoryExportController {

    private final ExportTaskService exportTaskService;


    @PostMapping("/unified")
    public ResponseEntity<?> exportInventory(
            @RequestBody UnifiedExportRequest unifiedExportRequest, HttpServletRequest request) {
        try {
            log.info("submit async export task, type: {}, format: {}",
                    unifiedExportRequest.getUnifiedType(), unifiedExportRequest.getFormat());

            String taskId = exportTaskService.submitExportTask(unifiedExportRequest, request);

            return ResponseEntity.ok(Result.ok(taskId));
        } catch (Exception e) {
            log.error("submit export task failed", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    //    @PostMapping("/lldp")
//    public ResponseEntity<?> exportLldpInventory(
//            @RequestBody UnifiedExportRequest unifiedExportRequest) {
//        unifiedExportRequest.setUnifiedType(InventoryType.LLDP);
//        return exportInventory(unifiedExportRequest);
//    }
//
//
//    @PostMapping("/ne")
//    public ResponseEntity<?> exportNeInventory(
//            @RequestBody UnifiedExportRequest unifiedExportRequest) {
//        unifiedExportRequest.setUnifiedType(InventoryType.NE);
//        return exportInventory(unifiedExportRequest);
//    }
//
//
//    @PostMapping("/tunnel")
//    public ResponseEntity<?> exportTunnelInventory(
//            @RequestBody UnifiedExportRequest unifiedExportRequest) {
//        unifiedExportRequest.setUnifiedType(InventoryType.TUNNEL);
//        return exportInventory(unifiedExportRequest);
//    }
//
//
//    @PostMapping("/transceiver")
//    public ResponseEntity<?> exportSnInventory(
//            @RequestBody UnifiedExportRequest unifiedExportRequest) {
//        unifiedExportRequest.setUnifiedType(InventoryType.TRANSCEIVER);
//        return exportInventory(unifiedExportRequest);
//    }
//
//
    @GetMapping("/result/{taskId}")
    public ResponseEntity<?> getExportResult(@PathVariable String taskId) {
        ExportTaskDto taskDto = exportTaskService.getTaskStatus(taskId);
        if (taskDto == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "export task not found: " + taskId);
        }
        return ResponseEntity.ok(Result.ok(taskDto));
    }

    @GetMapping("/preview/{taskId}")
    public ResponseEntity<?> previewExportData(@PathVariable String taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String keyword) {

        ExportResultPreviewDto exportResultPreviewDto = exportTaskService.getExportResultPreviewDto(
                taskId, page, limit, keyword);
        return ResponseEntity.ok(Result.ok(exportResultPreviewDto));
    }

    @GetMapping("/download/{taskId}")
    public void downloadExportFile(@PathVariable String taskId,
            HttpServletResponse response) {
        ExportTaskDto taskDto = exportTaskService.getTaskStatus(taskId);
        if (taskDto == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "export task not found: " + taskId);
        }
        if (taskDto.getStatus() != ExportTaskStatus.COMPLETED) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "export task is not completed, current status: " + taskDto.getStatus());
        }

        Path filePath = exportTaskService.getTaskFilePath(taskId);
        if (filePath == null || !Files.exists(filePath)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "export file not found: " + taskId);
        }

        String fileName = filePath.getFileName().toString();
        String contentType = fileName.endsWith(".csv")
                ? "text/csv;charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache,no-store,must-revalidate");

        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            response.getOutputStream().flush();
            log.info("export file downloaded: taskId={}, file={}", taskId, fileName);
        } catch (IOException e) {
            log.error("download export file failed: taskId={}, file={}", taskId, fileName, e);
            if (e.getMessage() != null &&
                    (e.getMessage().contains("Broken pipe") ||
                            e.getMessage().contains("Connection reset"))) {
                log.info("Client disconnected during download, taskId={}", taskId);
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "download export file failed", e);
            }
        }
    }
}
