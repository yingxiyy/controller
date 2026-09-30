package net.flex.dci.otn.controller.resource.statistic.core.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;

/**
 * Excel 导出工具类 基于 EasyExcel 实现
 *
 * @author musa
 * @version 1.0
 * @date 2026/4/4
 **/
@Slf4j
public class ExcelUtil {


    public static <T> void exportBeansToHttpResponse(List<T> beans, String filename,
            HttpServletResponse response, Class<T> clazz) {
        exportBeansToHttpResponse(beans, filename, "Sheet1", response, clazz);
    }


    public static <T> void exportBeansToHttpResponse(List<T> beans, String filename,
            String sheetName, HttpServletResponse response, Class<T> clazz) {
        setupExcelResponse(response, filename);
        try {
            ExcelWriterBuilder writerBuilder = EasyExcel.write(response.getOutputStream(), clazz)
                    .registerWriteHandler(new FontStyleWriteHandler())  // 设置字体为 Arial
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())  // 自适应列宽
                    .registerWriteHandler(new FirstColumnWidthStyleStrategy());  // 第一列固定宽度

            if (beans.isEmpty()) {
                writerBuilder.sheet(sheetName).doWrite(java.util.Collections.emptyList());
            } else {
                writerBuilder.sheet(sheetName).doWrite(beans);
            }

            log.debug("success to export beans to excel file:{} sheet:{} total:{}",
                    filename, sheetName, beans.size());
        } catch (IOException e) {
            log.error("failed to export beans to excel file:{}", filename, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to export beans to excel file reason is:" + e.getMessage(), e);
        }
    }


    /**
     * export bean to excel file
     *
     * @param beans data
     * @param filePath target file name
     * @param sheetName sheetName
     * @param clazz data struct class
     */
    public static void writeBeansToExcelFile(List<?> beans, String filePath,
            String sheetName, Class<?> clazz) {
        try (OutputStream os = new FileOutputStream(filePath)) {
            ExcelWriterBuilder writerBuilder = EasyExcel.write(os, clazz)
                    .registerWriteHandler(new FontStyleWriteHandler())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(new FirstColumnWidthStyleStrategy());

            if (beans == null || beans.isEmpty()) {
                writerBuilder.sheet(sheetName != null ? sheetName : "Sheet1")
                        .doWrite(java.util.Collections.emptyList());
            } else {
                writerBuilder.sheet(sheetName != null ? sheetName : "Sheet1")
                        .doWrite(beans);
            }
            log.debug("success to write beans to excel file:{} sheet:{} total:{}",
                    filePath, sheetName, beans != null ? beans.size() : 0);
        } catch (IOException e) {
            log.error("failed to write beans to excel file:{}", filePath, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to write beans to excel file reason is:" + e.getMessage(), e);
        }
    }

    /**
     * 设置 Excel 响应头
     */
    private static void setupExcelResponse(HttpServletResponse response, String filename) {
        try {
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("\\+", "%20");
            String contentDisposition = String.format(
                    "attachment; filename=\"%s.xlsx\"; filename*=UTF-8''%s.xlsx",
                    filename, encodedFilename);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache,no-store,must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setHeader(HttpHeaders.EXPIRES, "0");
        } catch (UnsupportedEncodingException e) {
            log.error("set up excel response failed", e);
            throw new RuntimeException("set up excel response failed", e);
        }
    }


    /**
     * 获取导出文件扩展名
     */
    public static String getFileExtension(String format) {
        if (format == null) {
            return ".csv";
        }
        switch (format.toUpperCase()) {
            case "EXCEL":
            case "XLSX":
                return ".xlsx";
            case "CSV":
            default:
                return ".csv";
        }
    }


    /**
     * 获取导出文件 Content-Type
     */
    public static String getContentType(String format) {
        if (format == null) {
            return "text/csv;charset=UTF-8";
        }
        switch (format.toUpperCase()) {
            case "EXCEL":
            case "XLSX":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV":
            default:
                return "text/csv;charset=UTF-8";
        }
    }


    /**
     * 字体样式处理器 - 设置默认字体为 Arial，避免 Times 字体警告
     */
    public static class FontStyleWriteHandler implements SheetWriteHandler {

        @Override
        public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                WriteSheetHolder writeSheetHolder) {
            Workbook workbook = writeWorkbookHolder.getWorkbook();

            // 创建 Arial 字体
            Font font = workbook.createFont();
            font.setFontName("Arial");

            // 应用到整个工作簿的默认样式
            workbook.getCellStyleAt(0).setFont(font);
        }
    }


    /**
     * 第一列宽度策略：固定窄宽度，并设置为只读
     */
    public static class FirstColumnWidthStyleStrategy extends AbstractColumnWidthStyleStrategy {

        private static final int FIRST_COLUMN_WIDTH = 1;

        @Override
        protected void setColumnWidth(WriteSheetHolder writeSheetHolder,
                List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex,
                Boolean isHead) {

            Sheet sheet = writeSheetHolder.getSheet();
            int columnIndex = cell.getColumnIndex();

            // 第一列使用固定窄宽度
            if (columnIndex == 0) {
                sheet.setColumnWidth(0, FIRST_COLUMN_WIDTH);

                // 设置第一列为锁定（不可编辑）
                cell.getCellStyle().setLocked(true);
            }
        }
    }
}
