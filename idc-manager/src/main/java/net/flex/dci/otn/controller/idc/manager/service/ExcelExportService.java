package net.flex.dci.otn.controller.idc.manager.service;

import javax.servlet.http.HttpServletResponse;

/**
 * @version 1.0
 * @date 2022/1/14 16:20
 */
public interface ExcelExportService {

    void exportIdcTemplate(HttpServletResponse response) throws Exception;

    void exportIdcData(HttpServletResponse response) throws Exception;
}
