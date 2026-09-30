package net.flex.dci.otn.controller.idc.manager.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @version 1.0
 * @date 2022/1/28 14:18
 */
public class ExcelUtils {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static void writeExcel(HttpServletResponse response, List<? extends Object> data,
            String fileName, String sheetName, Class clazz) throws Exception {
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);
        HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(
                headWriteCellStyle, contentWriteCellStyle);
        EasyExcel.write(getOutputStream(fileName, response), clazz).excelType(ExcelTypeEnum.XLSX)
                .sheet(sheetName).registerWriteHandler(horizontalCellStyleStrategy).doWrite(data);
    }

    public static void writeExcel(HttpServletResponse response,
            String fileName) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource(
                IdcConstants.TEMPLATE_DIR_PATH + fileName + IdcConstants.TEMPLATE_SUFFIX);
        try (InputStream templateStream = classPathResource.getInputStream();
                Workbook workbook = WorkbookFactory.create(templateStream);
                OutputStream outputStream = getOutputStream(classPathResource.getFilename(),
                        response)) {
            workbook.write(outputStream);
        }
    }

    /**
     * fill data to export the excel with template
     */
    public static void writeExcelWithTemplate(HttpServletResponse response, String templateName,
            String sheetName, int dataStartRowIndex, int columnCount, List<List<Object>> dataRows,
            String outputFileName) throws Exception {
        ClassPathResource template = new ClassPathResource(
                IdcConstants.TEMPLATE_DIR_PATH + templateName + IdcConstants.TEMPLATE_SUFFIX);
        try (InputStream templateStream = template.getInputStream();
                Workbook workbook = WorkbookFactory.create(templateStream)) {
            fillTemplate(workbook, sheetName, dataStartRowIndex, columnCount, dataRows);
            try (OutputStream outputStream = getOutputStream(outputFileName, response)) {
                workbook.write(outputStream);
            }
        }
    }

    /**
     * fill the template
     *
     * @param workbook
     * @param sheetName
     * @param dataStartRowIndex
     * @param columnCount
     * @param dataRows
     */
    public static void fillTemplate(Workbook workbook, String sheetName, int dataStartRowIndex,
            int columnCount, List<List<Object>> dataRows) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalStateException(
                    "can not find the sheet [" + sheetName + "] in the excel template");
        }

        int sampleRowCount = countFilledRows(sheet, dataStartRowIndex);
        Row styleRow = sheet.getRow(dataStartRowIndex + sampleRowCount);
        CellStyle dataStyle = styleRow == null ? null : styleRow.getCell(0).getCellStyle();

        for (int i = 0; i < dataRows.size(); i++) {
            Row row = obtainRow(sheet, dataStartRowIndex + i, styleRow);
            List<Object> values = dataRows.get(i);
            for (int column = 0; column < columnCount; column++) {
                Cell cell = obtainCell(row, column);
                applyStyle(cell, dataStyle);
                setCellValue(cell, column < values.size() ? values.get(column) : null);
            }
        }

        for (int i = dataRows.size(); i < sampleRowCount; i++) {
            Row row = sheet.getRow(dataStartRowIndex + i);
            if (row == null) {
                continue;
            }
            if (styleRow != null && styleRow.getHeight() > 0) {
                row.setHeight(styleRow.getHeight());
            }
            for (int column = 0; column < columnCount; column++) {
                Cell cell = row.getCell(column);
                if (cell == null) {
                    continue;
                }
                applyStyle(cell, dataStyle);
                cell.setBlank();
            }
        }
    }

    private static int countFilledRows(Sheet sheet, int startRowIndex) {
        int count = 0;
        for (int i = startRowIndex; i <= sheet.getLastRowNum(); i++) {
            if (isBlankRow(sheet.getRow(i))) {
                break;
            }
            count++;
        }
        return count;
    }

    private static boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                continue;
            }
            CellType cellType = cell.getCellType();
            if (cellType == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()) {
                return false;
            }
            if (cellType == CellType.NUMERIC || cellType == CellType.BOOLEAN) {
                return false;
            }
        }
        return true;
    }

    private static Row obtainRow(Sheet sheet, int rowIndex, Row styleRow) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        if (styleRow != null && styleRow.getHeight() > 0) {
            row.setHeight(styleRow.getHeight());
        }
        return row;
    }

    private static Cell obtainCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? row.createCell(columnIndex) : cell;
    }

    private static void applyStyle(Cell cell, CellStyle style) {
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private static OutputStream getOutputStream(String fileName, HttpServletResponse response)
            throws Exception {
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setCharacterEncoding("utf8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + encodedName + ";filename*=UTF-8''" + encodedName);
        return response.getOutputStream();
    }

}
