package net.flex.dci.otn.controller.idc.manager.controller;

import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.service.ExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @version 1.0
 * @date 2022/1/14 15:34
 */
@Controller
@RequestMapping("/idc")
@Slf4j
public class TemplateController {

    @Autowired
    private ExcelExportService exportService;

    @RequestMapping(value = "/download_template", method = RequestMethod.GET)
    public void downExcelTemplate(HttpServletResponse response) {
        log.debug("start to download excel template ");
        try {
            exportService.exportIdcTemplate(response);
        } catch (Exception exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    exception.getMessage());
        }
    }
}
