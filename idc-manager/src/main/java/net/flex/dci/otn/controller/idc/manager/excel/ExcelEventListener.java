package net.flex.dci.otn.controller.idc.manager.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.dto.RowError;
import net.flex.dci.otn.controller.idc.manager.excel.validator.ExcelValidator;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/1/20 10:29
 */
@Slf4j
public class ExcelEventListener<T> extends AnalysisEventListener<T> {

    private static final Integer MAX_SIZE = 10000;

    private static final int SHOW_ROWS = 5;

    private final ExcelValidator<T> excelValidator;

    private final Consumer<Collection<T>> batchConsumer;

    private final Integer skipRows;

    private final List<T> list = new ArrayList<>();

    private int currentRow = 0;

    public ExcelEventListener(ExcelValidator<T> excelValidator,
            Consumer<Collection<T>> batchConsumer, Integer skipRows) {
        this.batchConsumer = batchConsumer;
        this.excelValidator = excelValidator;
        this.skipRows = skipRows != null ? skipRows : 0;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        list.clear();
        log.error("failed when batch consumer the excel", exception);
        throw exception;
    }

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        currentRow++;
        if (currentRow <= skipRows) {
            return;
        }
        if (data instanceof IdcData) {
            ((IdcData) data).setPhysicalRow(analysisContext.readRowHolder().getRowIndex() + 1);
        }
        if (isEmptyRow(data)) {
            return;
        }
        if (list.size() > MAX_SIZE) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "A single upload numbers can not more than " + MAX_SIZE);
        }
        list.add(data);
    }

    private boolean isEmptyRow(T data) {
        if (data == null) {
            return true;
        }
        try {
            for (java.lang.reflect.Field field : data.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(data);
                if (value != null && !value.toString().trim().isEmpty()) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Failed to check empty row", e);
        }
        return true;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (!CollectionUtils.isEmpty(this.list)) {
            List<RowError> errors = this.excelValidator.validateRow(this.list);
            int totalRows = this.list.size();
            if (!errors.isEmpty()) {
                String human = buildHumanMessage(totalRows, errors);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, human);
            }
            this.batchConsumer.accept(this.list);
//            List<String> validated = this.excelValidator.validate(this.list);
//            if (CollectionUtils.isEmpty(validated)) {
//                this.batchConsumer.accept(this.list);
//            } else {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        JSON.toJSONString(validated));
//            }
        }
    }

    private String buildHumanMessage(int totalRows, List<RowError> errors) {
        String totalLabel = totalRows == 1 ? "row" : "rows";
        if (errors.isEmpty()) {
            return "Import failed: " + totalRows + " " + totalLabel
                    + " in total, no data was imported.";
        }
        Map<Integer, List<RowError>> byRow = errors.stream()
                .collect(Collectors.groupingBy(RowError::getRowNumber,
                        LinkedHashMap::new, Collectors.toList()));
        int invalidRows = byRow.size();

        StringBuilder sb = new StringBuilder();
        sb.append("Import failed: ").append(totalRows).append(" ").append(totalLabel)
                .append(" in total, ").append(invalidRows).append(" ")
                .append(invalidRows == 1 ? "row is" : "rows are")
                .append(" invalid, no data was imported.\n");

        int shown = 0;
        for (Map.Entry<Integer, List<RowError>> entry : byRow.entrySet()) {
            if (shown >= SHOW_ROWS) {
                break;
            }
            String detail = entry.getValue().stream()
                    .map(RowError::getMessage)
                    .collect(Collectors.joining("; "));
            sb.append("Row ").append(entry.getKey()).append(": ").append(detail).append("\n");
            shown++;
        }
        if (invalidRows > shown) {
            int more = invalidRows - shown;
            sb.append("...and ").append(more).append(more == 1 ? " more row." : " more rows.");
        }

        return sb.toString();
    }
}
