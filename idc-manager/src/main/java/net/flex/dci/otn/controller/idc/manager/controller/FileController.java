package net.flex.dci.otn.controller.idc.manager.controller;

import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.service.ExcelExportService;
import net.flex.dci.otn.controller.idc.manager.service.ImportService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version 1.0
 * @date 2022/1/17 13:54
 */
@Slf4j
@RestController
@RequestMapping(value = "/idc")
public class FileController {

    @Autowired
    private ImportService importService;

    @Autowired
    private ExcelExportService exportService;


    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleSiteInfoUpload(@RequestPart(value = "file") MultipartFile file) {
        try {
            importService.importSiteInfo(file);
            return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.EXCEL_ERROR, ex.getMessage());
        }
    }


    @GetMapping(value = "/export")
    public void exportCurrentSiteInfo(HttpServletResponse response) {
        try {
            exportService.exportIdcData(response);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

}
