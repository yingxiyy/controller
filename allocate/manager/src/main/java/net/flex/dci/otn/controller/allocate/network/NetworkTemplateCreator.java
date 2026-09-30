/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.network.template.RoadmSheetService;
import net.flex.dci.otn.controller.allocate.network.template.SiteLinkRelationSheetData;
import net.flex.dci.otn.controller.allocate.network.template.SiteLinkSheetData;
import net.flex.dci.otn.controller.allocate.network.template.SiteLinkSheetService;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.SiteLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.WssLinks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkTemplateCreator {

    public static final String ROADMS_SHEET_NAME = "roadms";
    @Autowired
    SiteLinkSheetService siteLinkSheetService;
    @Autowired
    RoadmSheetService roadmSheetService;

    public void doIt(CreateNetworkInput input, HttpServletResponse response) {
        String fileName = "network" + System.currentTimeMillis() + ".xlsx";
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(
                    getOutputStream(fileName, response)).build();
        } catch (Exception e) {
            log.error("Failed to create templ for :{}", input, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to get excelWriter", e);
        }

        //write siteLink data to excel, one siteLink per sheet
        List<SiteLinks> siteLinks = input.getSiteLinks();
        Map<String, String> wssEquipMap = new HashMap<>();//<equipId,equipTypeVendorSpecific>
        for (SiteLinks siteLinkItem : siteLinks) {
            String friendlyName = siteLinkItem.getFriendlyName();
            WriteSheet writeSheet = EasyExcel.writerSheet(friendlyName).head(SiteLinkSheetData.class).build();
            Pair<List<SiteLinkSheetData>, Map<String, String>> data = siteLinkSheetService.createSheetData(siteLinkItem);
            List<SiteLinkSheetData> sheetData = data.getLeft();
            wssEquipMap.putAll(data.getRight());
            excelWriter.write(sheetData, writeSheet);

        }

        //write roadm data(siteLink relations) to one sheet
        WriteSheet roadmSheet = EasyExcel.writerSheet(ROADMS_SHEET_NAME).head(SiteLinkRelationSheetData.class).build();
        List<WssLinks> wssLinks = input.getWssLinks();
        List<SiteLinkRelationSheetData> roadmSheetDatas=new ArrayList<>();
        for (WssLinks wssLink : wssLinks) {
            SiteLinkRelationSheetData siteLinkRelationSheetData = roadmSheetService.createSheetData(wssLink,wssEquipMap);
            roadmSheetDatas.add(siteLinkRelationSheetData);
        }
        excelWriter.write(roadmSheetDatas, roadmSheet);

        excelWriter.finish();

    }

    private OutputStream getOutputStream(String fileName, HttpServletResponse response)
            throws Exception {
        fileName = URLEncoder.encode(fileName, "UTF-8");
        response.setContentType("application/vnd.ms-excel.sheet.macroEnabled.12");
        response.setCharacterEncoding("utf8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        return response.getOutputStream();
    }
}
