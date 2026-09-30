package net.flex.dci.otn.controller.resource.statistic.export.task;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.CSV_SUFFIX;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.EXCEL_SUFFIX;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ExportFormat;
import net.flex.dci.otn.controller.resource.statistic.core.utils.CsvUtil;
import net.flex.dci.otn.controller.resource.statistic.core.utils.ExcelUtil;
import net.flex.dci.otn.controller.resource.statistic.dto.ExportResultPreviewDto;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.export.file.ExportFileService;
import net.flex.dci.otn.controller.resource.statistic.properties.ExportFileProperties;
import net.flex.dci.otn.controller.resource.statistic.repository.TaskJsonRepository;
import net.flex.dci.otn.controller.resource.statistic.service.InventoryService;
import net.flex.dci.otn.controller.resource.statistic.task.ExportTaskManager;
import net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 导出任务管理服务实现 — 内存任务追踪 + 异步文件生成
 *
 * @author musa
 * @version 1.0
 * @date 2026/6/11
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportTaskServiceImpl implements ExportTaskService {

    private final InventoryService inventoryService;
    private final ExportFileService exportFileService;
    private final TaskJsonRepository taskJsonRepository;
    private final ExportFileProperties exportFileProperties;

    @Qualifier("exportExecutor")
    private final ThreadPoolTaskExecutor exportExecutor;

    private final ExportTaskManager exportTaskManager;

//    private final ConcurrentHashMap<String, ExportTaskHolder> taskMap = new ConcurrentHashMap<>();

    @Override
    public String submitExportTask(UnifiedExportRequest exportRequest, HttpServletRequest request) {

        String taskId = UUID.randomUUID().toString();
        String operator = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        String fileName = CommonUtils.exportCsvFileName(exportRequest);
        ExportTaskDto holder = ExportTaskDto.builder()
                .taskId(taskId)
                .exportRequest(exportRequest)
                .taskName(exportRequest.getUnifiedType() + "_" + exportRequest.getFormat() + "_"
                        + System.currentTimeMillis())
                .operator(operator)
                .fileName(fileName)
                .status(ExportTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .createTimestamp(System.currentTimeMillis())
                .build();
        taskJsonRepository.save(holder);

        CompletableFuture.runAsync(() -> processExport(taskId), exportExecutor);

        log.info("export task submitted: taskId={}, type={}, format={}",
                taskId, exportRequest.getUnifiedType(), exportRequest.getFormat());
        return taskId;
    }

    @Override
    public ExportTaskDto getTaskStatus(String taskId) {
        ExportTaskDto holder = taskJsonRepository.findById(taskId);
        if (holder == null) {
            return null;
        }
        return ExportTaskDto.builder()
                .taskId(holder.getTaskId())
                .status(holder.getStatus())
                .fileName(holder.getFileName())
                .errorMessage(holder.getErrorMessage())
                .createTime(holder.getCreateTime())
                .completeTime(holder.getCompleteTime())
                .build();
    }

    @Override
    public Path getTaskFilePath(String taskId) {
        ExportTaskDto holder = taskJsonRepository.findById(taskId);
        if (holder == null || holder.getStatus() != ExportTaskStatus.COMPLETED) {
            return null;
        }
        return Paths.get(holder.getFilePath());
    }

    @Override
    public ExportResultPreviewDto getExportResultPreviewDto(String taskId, int page,
            int limit, String keyword) {
        ExportTaskDto taskDto = taskJsonRepository.findById(taskId);
        if (taskDto == null) {
            log.warn("export task not found or expired : " + taskId);
            return ExportResultPreviewDto.builder().expired(true).build();
        }
        log.info("preview export result the file name is:{}", taskDto.getFileName());
        Path filePath = Paths.get(taskDto.getFilePath());
        if (!Files.exists(filePath)) {
            log.error(
                    "export file expired，file keep " + exportFileProperties.getTtl() + " day");
            return ExportResultPreviewDto.builder().expired(true).build();
        }
        List<List<String>> allRows = new ArrayList<>();
        List<String>[] headersRef = new List[1];
        final int[] maxColumnIndex = {0};
        final boolean[] isFirstRowProcessed = {false}; // 标记第一行是否已处理为表头

        EasyExcel.read(filePath.toFile(), new AnalysisEventListener<Map<Integer, Object>>() {

                    @Override
                    public void invoke(Map<Integer, Object> rowMap, AnalysisContext analysisContext) {
                        if (!isFirstRowProcessed[0]) {
                            int maxColumnIndex = rowMap.keySet().stream()
                                    .mapToInt(Integer::intValue)
                                    .max()
                                    .orElse(0);

                            List<String> headers = new ArrayList<>(maxColumnIndex + 1);
                            for (int i = 1; i <= maxColumnIndex; i++) {
                                Object cellValue = rowMap.get(i);
                                headers.add(cellValue != null ? cellValue.toString().trim() : "");
                            }

                            headersRef[0] = headers;
                            isFirstRowProcessed[0] = true;
                            log.info(" Successfully read headers from row 0: {}", headers);
                            return;
                        }
                        int currentMax = rowMap.keySet().stream().mapToInt(Integer::intValue).max()
                                .orElse(0);
                        if (currentMax > maxColumnIndex[0]) {
                            maxColumnIndex[0] = currentMax;
                        }

                        List<String> row = new ArrayList<>(maxColumnIndex[0] + 1);
                        for (int i = 1; i <= maxColumnIndex[0]; i++) {
                            Object cellValue = rowMap.get(i);
                            row.add(cellValue != null ? cellValue.toString() : "");
                        }

                        // 处理表头
                        if (headersRef[0] == null) {
                            headersRef[0] = row;
                            return;
                        }

                        if (keyword != null && !keyword.isEmpty()) {
                            boolean match = row.stream()
                                    .anyMatch(cell -> cell != null && cell.contains(keyword));
                            if (!match) {
                                return;
                            }
                        }

                        allRows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        log.debug("preview filtered rows: {}", allRows.size());
                    }
                }).sheet(0)
                .headRowNumber(0)
                .doRead();
        int total = allRows.size();
        int from = page * limit;
        int to = Math.min(from + limit, total);
        List<List<String>> pageRows = from < total ? allRows.subList(from, to) : new ArrayList<>();

        return ExportResultPreviewDto.builder()
                .headers(headersRef[0])
                .pageRows(pageRows)
                .total((long) total)
                .page((long) page)
                .limit((long) limit)
                .build();
    }


    private void processExport(String taskId) {
        ExportTaskDto holder = taskJsonRepository.findById(taskId);
        if (holder == null) {
            log.warn("export task {} not found, aborting", taskId);
            return;
        }

        holder.setStatus(ExportTaskStatus.PROCESSING);
        exportTaskManager.onTaskSubmitted(holder, holder.getOperator());
        log.info("start processing export task: {}", holder.getTaskId());

        try {

            UnifiedExportRequest exportRequest = holder.getExportRequest();
//            HttpServletRequest httpRequest = holder.getRequest();
            InventoryExportData<?> exportData = inventoryService.getUnifiedExportData(
                    exportRequest);

            ExportFormat format = exportRequest.getFormat();
            if (format == null) {
                format = ExportFormat.EXCEL;
            }
            String suffix = (format == ExportFormat.CSV) ? CSV_SUFFIX : EXCEL_SUFFIX;

            Path filePath = exportFileService.generateExportFilePath(
                    exportData.getFilename(), suffix);

            if (exportData.getExportDatas() == null || exportData.getExportDatas().isEmpty()) {
                log.info("export: no data to export, writing empty file");
            }

            if (format == ExportFormat.CSV) {
                CsvUtil.writeBeansToCsv(filePath.toString(), exportData.getExportDatas());
            } else {
                ExcelUtil.writeBeansToExcelFile(
                        exportData.getExportDatas(),
                        filePath.toString(),
                        exportData.getSheetName(),
                        exportData.getClazz());
            }

            long fileSize = Files.size(filePath);
            holder.setStatus(ExportTaskStatus.COMPLETED);
            holder.setFilePath(filePath.toAbsolutePath().toString());
            holder.setFileName(exportData.getFilename());
            holder.setCompleteTime(LocalDateTime.now());
            holder.setCompleteTimestamp(System.currentTimeMillis());
            log.info("export completed: file={}, size={} bytes, records={}",
                    filePath, fileSize,
                    exportData.getExportDatas() != null ? exportData.getExportDatas().size() : 0);

        } catch (Exception e) {
            log.error("export task  failed", e);
            holder.setStatus(ExportTaskStatus.FAILED);
            holder.setErrorMessage(e.getMessage());
            holder.setCompleteTime(LocalDateTime.now());
            holder.setCompleteTimestamp(System.currentTimeMillis());
        } finally {
            taskJsonRepository.save(holder);
            exportTaskManager.onTaskCompleted(holder);
        }
    }


}
