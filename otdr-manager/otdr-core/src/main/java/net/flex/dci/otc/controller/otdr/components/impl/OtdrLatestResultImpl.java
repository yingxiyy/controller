package net.flex.dci.otc.controller.otdr.components.impl;

import static net.flex.dci.otc.common.constants.AuthConstant.BLANK;
import static net.flex.dci.otc.common.constants.Constants.POUND;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.OtdrLatestResult;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResult;
import net.flex.dci.otc.controller.otdr.model.OtdrCurrentDetail;
import net.flex.dci.otc.controller.otdr.model.OtdrResult;
import net.flex.dci.otc.controller.otdr.model.OtdrResultOutput;
import net.flex.dci.otc.controller.otdr.model.link.PhyLinkInfo;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.OtdrResultRecordDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/1 13:56
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrLatestResultImpl implements OtdrLatestResult {

    private final OtdrDaoService otdrResultRecordDao;

    private final PhyLinkDao phyLinkDao;

    private final PhyNodeDao phyNodeDao;

    private final TerminationPointDao terminationPointDao;

    @Override
    public OtdrResultOutput getLatestOtdrResultByMonitorTpId(String monitorTpId) {
        log.debug("start to get the latest otdr result for the monitor port:{}", monitorTpId);
        OtdrResultRecordDto otdrResultRecordDto = otdrResultRecordDao.getLatestResultByTpId(
                monitorTpId);
        if (otdrResultRecordDto == null) {
            return OtdrResultOutput.builder().build();
        }
        OtdrResultOutput otdrResultOutput = OtdrResultOutput.builder()
                .tpId(monitorTpId)
                .content(otdrResultRecordDto.getContent())
                .scanMode(otdrResultRecordDto.getScanMode())
                .scanParameters(JSONObject.parse(otdrResultRecordDto.getScanParameters())).build();

        return otdrResultOutput;
    }

    /**
     * get latest otdr res by monitor tp id both change
     *
     * @param monitorTpId
     * @return
     */
    @Override
    public LatestOtdrResult getLatestOtdrResByMonitorTpId(String monitorTpId) {
        log.debug("get the latest otdr result for monitor tp :{},direction is z-a and a-z",
                monitorTpId);
//        Link phyLink = getRefPhyLink(monitorTpId);
//        if (phyLink == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("there is no available link for the monitor tp %s ",
//                            monitorTpId));
//        }
//        boolean isDestinationPort = isDestinationPort(monitorTpId, phyLink);
//        PhyLinkInfo phyLinkInfo = OtdrUtils.getPhyLinkBrieflyInfo(phyLink);
//        OtdrResult azResult = getLatestOtdrResultByLink(phyLinkInfo, OtdrMonitorDirection.AZ);
//        OtdrResult zaResult = getLatestOtdrResultByLink(phyLinkInfo, OtdrMonitorDirection.ZA);

        OtdrResult outDetail = getLatestOtdrResult(monitorTpId, MonitorDirection.OUT);
        OtdrResult inDetail = getLatestOtdrResult(monitorTpId, MonitorDirection.IN);

        log.debug("finish to get the latest otdr result for monitor tp :{}", monitorTpId);

        // 根据端口角色决定 in/out 的映射关系
        // 目的端口：AZ方向为IN，ZA方向为OUT；源端口：AZ方向为OUT，ZA方向为IN
//        OtdrResult inDetail = isDestinationPort ? azResult : zaResult;
//        OtdrResult outDetail = isDestinationPort ? zaResult : azResult;

        log.debug("finish to get the latest otdr result for monitor tp :{}", monitorTpId);
        if (inDetail == null && outDetail == null) {
            return null;
        }
        return LatestOtdrResult.builder()
                .in(inDetail == null ? OtdrUtils.getDefaultOTDRDetail(monitorTpId,
                        MonitorDirection.IN)
                        : refactorOtdrResult(inDetail, monitorTpId, MonitorDirection.IN))
                .out(outDetail == null ? OtdrUtils.getDefaultOTDRDetail(monitorTpId,
                        MonitorDirection.OUT)
                        : refactorOtdrResult(outDetail, monitorTpId, MonitorDirection.OUT))
                .build();
    }

    private boolean isDestinationPort(String monitorTpId, Link phyLink) {
        log.debug("detective the current port:{} is destination on phy link:{} or not ",
                monitorTpId, phyLink.getLinkId());
        return monitorTpId.equals(phyLink.getDestination().getDestTp().getValue());
    }

    private OtdrCurrentDetail refactorOtdrResult(OtdrResult otdrResult, String monitorTpId,
            MonitorDirection monitorDirection) {
        OtdrCurrentDetail currentDetail = otdrResult.getDetail();
        log.debug(
                "refactor OTDR result refactor from original tp :{} monitorDirection:{} to  monitorDirection:{} and monitorPort:{}",
                currentDetail.getMonitorDirection(), currentDetail.getMonitorPort(),
                monitorDirection, monitorTpId);
        String refNeId = PhysicalTpIdNamingRule.getNodeId(monitorTpId);
        String refNeName = phyNodeDao.getFriendlyName(refNeId);
        String refTpName = terminationPointDao.getTpPhysical(refNeId, monitorTpId)
                .getFriendlyName();
        String monitorTpName = refNeName + POUND + refTpName;
        currentDetail.setMonitorDirection(monitorDirection.name());
        currentDetail.setMonitorPortName(monitorTpName);
        currentDetail.setMonitorNeId(refNeId);
        currentDetail.setMonitorNeName(refNeName);
        currentDetail.setMonitorPort(monitorTpId);
        return currentDetail;
    }

    private OtdrResult getLatestOtdrResultByLink(PhyLinkInfo phyLinkInfo,
            OtdrMonitorDirection otdrMonitorDirection) {
        log.debug("get monitor direction for the link :{},monitor direction is:{}",
                phyLinkInfo.getLinkId(), otdrMonitorDirection.getDirectionStr());
        String srcTp = phyLinkInfo.getSrcTpId();
        String destTp = phyLinkInfo.getDestTpId();
        OtdrResult otdrResult = null;
        if (otdrMonitorDirection.equals(OtdrMonitorDirection.AZ)) {
            OtdrResult azResultSrc = getLatestOtdrResult(srcTp, MonitorDirection.OUT);
            OtdrResult azResultDest = getLatestOtdrResult(destTp, MonitorDirection.IN);
            otdrResult = getLatestOtdrResult(azResultDest, azResultSrc);
        } else {
            OtdrResult zaResultSrc = getLatestOtdrResult(srcTp, MonitorDirection.IN);
            OtdrResult zaResultDest = getLatestOtdrResult(destTp, MonitorDirection.OUT);
            otdrResult = getLatestOtdrResult(zaResultSrc, zaResultDest);
        }
        return otdrResult;
    }

    /**
     * get latest otdr for the re/
     *
     * @param in
     * @param out
     * @return
     */
    private OtdrResult getLatestOtdrResult(OtdrResult in, OtdrResult out) {
        OtdrResult otdrResult = null;
        if (in == null && out == null) {
            otdrResult = null;
        } else if (in == null && out != null) {
            otdrResult = out;
        } else if (out == null && in != null) {
            otdrResult = in;
        } else {
            long inStartTimestamp = OtdrUtils.convertTimeString2Timestamp(
                    in.getDetail().getStartTime());
            long outStartTimestamp = OtdrUtils.convertTimeString2Timestamp(
                    out.getDetail().getStartTime());
            otdrResult = inStartTimestamp >= outStartTimestamp ? in : out;
        }
        return otdrResult;
    }

    public OtdrResult getLatestOtdrResult(String monitorTpId, MonitorDirection monitorDirection) {
        OtdrResultRecordDto result = otdrResultRecordDao.getLatestDirectionResultByMonitorTpId(
                monitorTpId,
                monitorDirection.getIntValue());
        String otdrResult = result == null ? BLANK : result.getContent();
        OtdrResult detail = JSON.parseObject(otdrResult, OtdrResult.class);
        return detail;
    }

    private Link getRefPhyLink(String monitorTypId) {
        log.debug("get ref phy link by monitor type id :{}", monitorTypId);
        List<Link> links = phyLinkDao.listAllPhyLinksUnderTp(monitorTypId);
        Optional<Link> refOtsLink = links.stream()
                .filter(link -> PhysicalLinkIdNamingRule.isOtsLink(link.getLinkId().getValue()))
                .findAny();
        return refOtsLink.orElse(null);
    }
}
