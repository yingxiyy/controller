package net.flex.dci.otn.controller.idc.manager.excel;

import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/1/19 17:09
 */
@Data
public class ExcelError implements Serializable {

    private int row;

    private int column;

    private String errorMsg;

    public ExcelError(int row, int column, String errorMsg) {
        this.row = row;
        this.column = column;
        this.errorMsg = errorMsg;
    }
}
