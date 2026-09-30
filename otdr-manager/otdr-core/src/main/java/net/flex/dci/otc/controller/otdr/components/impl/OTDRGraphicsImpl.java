package net.flex.dci.otc.controller.otdr.components.impl;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.BaseComponent;
import net.flex.dci.otc.controller.otdr.components.OTDRGraphics;
import net.flex.dci.otc.controller.otdr.model.OtdrCurrentDetail;
import net.flex.dci.otc.controller.otdr.model.OtdrResult;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrRes;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/20/2023 11:42 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRGraphicsImpl extends BaseComponent implements OTDRGraphics {

    private final OtdrDaoService otdrDaoService;

    private final TerminationPointDao terminationPointDao;

    private final PhyNodeDao phyNodeDao;

    @Override
    public List<ShowOtdrRes> showOtdrGraphicsByTaskIds(List<String> taskIds) {
        log.debug("get the otdr graphics by task id ,the task id is:{}", taskIds);
        List<OtdrResultRecord> otdrResults = otdrDaoService.listAllOtdrRecordByIds(
                taskIds);
        List<ShowOtdrRes> showOtdrResults = otdrResults.stream().map(this::convert2ShowOtdrRes)
                .collect(Collectors.toList());
        return showOtdrResults;
    }

    private ShowOtdrRes convert2ShowOtdrRes(OtdrResultRecord resultRecord) {
        log.debug("convert otdr result record to show otdr result");
        String detailResult = resultRecord.getContent();
        OtdrResult otdrResult = JSON.parseObject(detailResult, OtdrResult.class);
        String monitorTpId = resultRecord.getTpId();
        String refNeId = PhysicalTpIdNamingRule.getNodeId(monitorTpId);
        String refNeName = phyNodeDao.getFriendlyName(refNeId);
        String refTpName = terminationPointDao.getTpPhysical(refNeId, monitorTpId)
                .getFriendlyName();
        String monitorTpName = refNeName + POUND + refTpName;
        OtdrCurrentDetail otdrDetail = otdrResult.getDetail();
        Long startTimestamp = OtdrUtils.convertTimeString2Timestamp(otdrDetail.getStartTime());
        ShowOtdrRes showOtdrRes = ShowOtdrRes.builder()
                .monitorDirection(otdrDetail.getMonitorDirection())
                .monitorPort(monitorTpName)
                .monitorPortId(monitorTpId)
                .taskId(resultRecord.getId())
                .scanTime(otdrDetail.getScanTime())
                .monitorPortName(monitorTpName)
                .monitorNeId(refNeId)
                .monitorNeName(refNeName)
                .startTime(startTimestamp)
                .waveForm(otdrDetail.getWaveForm())
                .resultId(otdrDetail.getResultId())
                .build();
        return showOtdrRes;
    }
}
