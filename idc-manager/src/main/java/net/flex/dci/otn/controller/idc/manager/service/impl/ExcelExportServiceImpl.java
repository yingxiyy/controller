package net.flex.dci.otn.controller.idc.manager.service.impl;

import static net.flex.dci.otn.controller.idc.manager.utils.IdcConstants.TEMPLATE_NAME;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.idc.manager.service.ExcelExportService;
import net.flex.dci.otn.controller.idc.manager.utils.ConvertorUtils;
import net.flex.dci.otn.controller.idc.manager.utils.ExcelUtils;
import net.flex.dci.otn.controller.idc.manager.utils.IdcConstants;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import net.flex.dci.otn.db.jpa.service.dao.SiteInfoDaoService;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/1/14 16:15
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final String EXPORT_FILE_NAME_PREFIX = "idc_data";

    private final SiteInfoDaoService siteInfoDaoService;


    @Override
    public void exportIdcTemplate(HttpServletResponse response) throws Exception {
        log.debug("start to export idc info list excel template");
        ExcelUtils.writeExcel(response, TEMPLATE_NAME);
    }

    @Override
    public void exportIdcData(HttpServletResponse response) throws Exception {
        log.debug("start to export idc info list excel");
        List<SiteInfo> siteInfos = new ArrayList<>(siteInfoDaoService.findAll());
        siteInfos.sort(Comparator.comparing(SiteInfo::getId));
        log.info("start to export {} idc records into the excel template", siteInfos.size());

        List<List<Object>> dataRows = ConvertorUtils.convert2IdcExcelRows(siteInfos);
        ExcelUtils.writeExcelWithTemplate(response, TEMPLATE_NAME, IdcConstants.IDC_SHEET_NAME,
                IdcConstants.IDC_DATA_START_ROW, IdcConstants.IDC_COLUMN_COUNT, dataRows,
                buildExportFileName());
        log.info("finish to export {} idc records", dataRows.size());
    }

    private String buildExportFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return EXPORT_FILE_NAME_PREFIX + "_" + timestamp + IdcConstants.TEMPLATE_SUFFIX;
    }

}
