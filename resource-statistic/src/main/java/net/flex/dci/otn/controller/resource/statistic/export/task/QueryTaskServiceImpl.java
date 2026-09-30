package net.flex.dci.otn.controller.resource.statistic.export.task;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.EXCEL_SUFFIX;
import static net.flex.dci.otn.controller.resource.statistic.utils.UnifiedQueryParamResolver.generateQueryFileName;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otn.controller.resource.statistic.core.manager.QueryTaskManager;
import net.flex.dci.otn.controller.resource.statistic.core.utils.ExcelUtil.FirstColumnWidthStyleStrategy;
import net.flex.dci.otn.controller.resource.statistic.core.utils.ExcelUtil.FontStyleWriteHandler;
import net.flex.dci.otn.controller.resource.statistic.dto.query.QueryResultInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;
import net.flex.dci.otn.controller.resource.statistic.export.TunnelCsv;
import net.flex.dci.otn.controller.resource.statistic.export.file.ExportFileService;
import net.flex.dci.otn.controller.resource.statistic.repository.TaskJsonRepository;
import net.flex.dci.otn.controller.resource.statistic.task.ExportTaskManager;
import net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils;
import net.flex.dci.otn.controller.resource.statistic.utils.UnifiedQueryParamResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 2026/7/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class QueryTaskServiceImpl implements QueryTaskService {

    private final ExportFileService exportFileService;
    private final TaskJsonRepository taskJsonRepository;
    private final QueryTaskManager queryTaskManager;

    @Qualifier("exportExecutor")
    private final ThreadPoolTaskExecutor exportExecutor;

    private final ExportTaskManager exportTaskManager;


    @Override
    public String submitQueryTask(UnifiedResourceQueryParam unifiedResourceQueryParam,
            HttpServletRequest request) {
        log.info("submit query task the request is:{}", unifiedResourceQueryParam);
        String taskId = UUID.randomUUID().toString();
        String operator = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        String fileName = CommonUtils.exportQueryFileName(unifiedResourceQueryParam);
        ExportTaskDto holder = ExportTaskDto.builder()
                .taskId(taskId)
                .query(unifiedResourceQueryParam)
                .taskName(unifiedResourceQueryParam.getUnifiedType() + "_"
                        + System.currentTimeMillis())
                .operator(operator)
                .fileName(fileName)
                .status(ExportTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .createTimestamp(System.currentTimeMillis())
                .build();
        taskJsonRepository.save(holder);

        CompletableFuture.runAsync(() -> processQuery(taskId), exportExecutor);

        log.info("export task submitted: taskId={}, type={}",
                taskId, unifiedResourceQueryParam.getUnifiedType());
        return taskId;
    }

    @Override
    public String submitQueryTask(UnifiedQueryParam unifiedQueryParam, HttpServletRequest request) {
        log.info("submit query task the request is:{}", unifiedQueryParam);
        String taskId = UUID.randomUUID().toString();
        String operator = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        log.info("query operator is:{}", operator);
        String fileName = generateQueryFileName(unifiedQueryParam);
        String queryTaskName = CommonUtils.generateQueryTaskName(unifiedQueryParam);
        ExportTaskDto holder = ExportTaskDto.builder()
                .taskId(taskId)
                .queryParam(unifiedQueryParam)
                .taskName(queryTaskName)
                .fileName(fileName)
                .operator(operator)
                .status(ExportTaskStatus.PENDING)
                .createTime(LocalDateTime.now())
                .createTimestamp(System.currentTimeMillis())
                .build();
        taskJsonRepository.save(holder);
        CompletableFuture.runAsync(() -> processQuery(taskId), exportExecutor);
        log.info("query task submitted:task id={},type={}", taskId,
                unifiedQueryParam.getResourceQueryType());
        return taskId;
    }

    private void processQuery(String taskId) {
        ExportTaskDto holder = taskJsonRepository.findById(taskId);
        if (holder == null) {
            log.warn("export task {} not found, aborting", taskId);
            return;
        }

        holder.setStatus(ExportTaskStatus.PROCESSING);
        exportTaskManager.onQueryTaskSubmitted(holder, holder.getOperator());
        log.info("start processing export task: {}", holder.getTaskId());

        try {

            UnifiedQueryParam queryParam = holder.getQueryParam();
//            InventoryExportData<?> exportData = queryTaskManager.queryData(
//                    queryRequest);
            String suffix = EXCEL_SUFFIX;
            String fileName = holder.getFileName();
            Path filePath = exportFileService.generateExportFilePath(
                    fileName, suffix);
            holder.setFileName(fileName);

            writeToExcelStream(queryParam, filePath);

            long fileSize = Files.size(filePath);
            holder.setStatus(ExportTaskStatus.COMPLETED);
            holder.setFileSize(fileSize);
            holder.setFilePath(filePath.toAbsolutePath().toString());

            holder.setCompleteTime(LocalDateTime.now());
            holder.setCompleteTimestamp(System.currentTimeMillis());

        } catch (Exception e) {
            log.error("export task  failed", e);
            holder.setStatus(ExportTaskStatus.FAILED);
            holder.setErrorMessage(e.getMessage());
            holder.setCompleteTime(LocalDateTime.now());
            holder.setCompleteTimestamp(System.currentTimeMillis());
        } finally {
            taskJsonRepository.save(holder);
            exportTaskManager.onQueryTaskCompleted(holder);
        }
    }

    private void writeToExcelStream(UnifiedQueryParam queryParam, Path filePath) {
        QueryResultInfo queryResultInfo = UnifiedQueryParamResolver.resolveQueryResult(queryParam);
        ExcelWriterBuilder excelWriterBuilder = EasyExcel.write(filePath.toFile(),
                        queryResultInfo.getClazz())
                .registerWriteHandler(new FontStyleWriteHandler())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        if (queryResultInfo.getClazz().equals(TunnelCsv.class)) {
            excelWriterBuilder.registerWriteHandler(new FirstColumnWidthStyleStrategy());
        }

        try (ExcelWriter excelWriter = excelWriterBuilder.build()) {
            AtomicInteger totalRecords = new AtomicInteger(0);
            WriteSheet writeSheet = EasyExcel.writerSheet(queryResultInfo.getSheetName()).build();
            queryTaskManager.queryStream(queryParam, pageData -> {
                excelWriter.write(pageData, writeSheet);
                totalRecords.addAndGet(pageData.size());
            });

            log.info("export completed: file={}, records={}",
                    filePath, totalRecords.get());
        }
    }


}
