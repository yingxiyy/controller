package net.flex.dci.otn.controller.idc.manager.service.impl;

import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.excel.ExcelReader;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.service.IDCDataUploadService;
import net.flex.dci.otn.controller.idc.manager.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version 1.0
 * @date 2022/1/17 14:22
 */
@RestController
@Slf4j
public class ImportServiceImpl implements ImportService {

    @Autowired
    private ExcelReader excelReader;
    //    @Autowired
//    private IdcDataService dataService;
    @Autowired
    private IDCDataUploadService uploadService;

    @Override
    public Object importSiteInfo(MultipartFile file) throws Exception {
        log.debug("start to import site info for the file name is {}", file.getName());
        if (file.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the file cannot be null");
        }
        InputStream inputStream = file.getInputStream();
        ExcelReader.Meta<IdcData> excelDataMeta = new ExcelReader.Meta<>();
        excelDataMeta.setExcelStream(inputStream);
        excelDataMeta.setDomain(IdcData.class);
        excelDataMeta.setHeadRowNumber(1);
        excelDataMeta.setSkipRows(2);
        excelDataMeta.setConsumer(uploadService::batchUploadIdcData);
        this.excelReader.read(excelDataMeta);
        return null;

    }
}
