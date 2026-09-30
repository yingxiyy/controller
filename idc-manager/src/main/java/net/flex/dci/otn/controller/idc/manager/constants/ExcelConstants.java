package net.flex.dci.otn.controller.idc.manager.constants;

/**
 * @version 1.0
 * @date 2022/1/19 10:41
 */
public class ExcelConstants {

    public static final String EXCEL_EXTENSION = ".xlsx";

    public static final Integer PER_SHEET_ROW_COUNT = 1000000;

    public static final Integer PER_WRITE_ROW_COUNT = 200000;

    public static final Integer PER_SHEET_WRITE_COUNT = PER_SHEET_ROW_COUNT / PER_WRITE_ROW_COUNT;

    public static final Integer PER_READ_INSERT_BATCH_COUNT = 100000;
}
