package net.flex.dci.otc.controller.otdr.components.impl;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import com.alibaba.fastjson.JSON;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.OTDRResult;
import net.flex.dci.otc.controller.otdr.components.ScanPortHelper;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import net.flex.dci.otc.controller.otdr.dto.OTDRLinkQueryDto;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.model.OtdrResult;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResult;
import net.flex.dci.otc.controller.otdr.model.link.WrappedLink;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsDirectionOtdrResult;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsLinkOtdrLatestResult;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsLinkTerminalInfo;
import net.flex.dci.otc.controller.otdr.topology.OTDRTopoHolder;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.mongo.dao.OtdrBaseBenchmarkDao;
import net.flex.dci.otc.mongo.mdoel.otdr.OtdrBaseBenchmark;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.OtdrResultRecordDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.OTDRPagedQueryDto;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.output.Result;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.output.ResultBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.output.ResultKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/1/9 12:21
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRResultImpl implements OTDRResult {

    private final OtdrDaoService otdrResultRecordDao;

    private final OtdrBaseBenchmarkDao otdrBaseBenchmarkDao;

    private final ScanPortHelper scanPortHelper;

    @Override
    public Page<Result> listByQueryItemPaged(OTDRLinkQueryDto otdrLinkQueryDto) {
        log.debug("query otdr result item by paged ");
        OTDRPagedQueryDto pagedQueryDto = otdrLinkQueryDto.getOtdrPagedQueryDto();
        Page<OtdrResultRecord> otdrResultRecordPage = otdrResultRecordDao.listAllOnConditionalParamPage(
                pagedQueryDto);
        OtdrBaseBenchmark otdrBaseBenchmark = otdrBaseBenchmarkDao.getOtdrBaseBenchmarkByLinkId(
                pagedQueryDto.getLinkId());
        Page<Result> resultPage = otdrResultRecordPage.map(
                otdrResult -> conver2OutputResult(otdrResult, otdrBaseBenchmark,
                        otdrLinkQueryDto.getNodeMap()));
        return resultPage;
    }

    @Override
    public OtdrResultRecord getOtdrDetailResultByTaskId(BigInteger taskId) {
        log.debug("query otdr result item by taskId");
        OtdrResultRecord otdrResultRecord = otdrResultRecordDao.findById(taskId.longValue());
        return otdrResultRecord;
    }

    @Override
    public Page<OtdrBrieflyResult> listResultByQueryItemPaged(OTDRLinkQueryDto linkQueryDto) {
        log.debug("query otdr result item by paged ");
        Page<OtdrResultRecord> otdrResultRecordPage = otdrResultRecordDao.listAllOnConditionalParamPage(
                linkQueryDto.getOtdrPagedQueryDto());
//        Link phyLink = phyLinkDao.getPhyLinkById(pagedQueryDto.getLinkId());
//        OtdrBaseBenchmark otdrBaseBenchmark = otdrBaseBenchmarkDao.getOtdrBaseBenchmarkByLinkId(
//                pagedQueryDto.getLinkId());
        Page<OtdrBrieflyResult> resultPage = otdrResultRecordPage.map(
                otdrResult -> convert2BrieflyOutputResult(otdrResult, linkQueryDto.getPhyLinkId(),
                        linkQueryDto.getProvider(), linkQueryDto.getNodeMap()));
        return resultPage;
    }

    @Override
    public List<OtsLinkOtdrLatestResult> getOtsLinksOtdrLatestResult(List<WrappedLink> otsLinks,
            OTDRTopoHolder otdrTopoHolder) {
        List<String> otsLinkIds = otsLinks.stream().map(WrappedLink::getLink)
                .map(LinkAttributes::getLinkId).map(Uri::getValue).collect(
                        Collectors.toList());
        log.debug(
                "get ots links latest otdr direction from a to z and z to a,the ots link ids is:{}",
                otsLinkIds);
        Set<String> otdrPorts = otdrTopoHolder.getOtdrTps();
        Map<String, OtdrResultRecordDto> latestOTDRByTp = buildOtdrPortLatestOTDR(otdrPorts);

        List<OtsLinkOtdrLatestResult> otsLinkOtdrLatestResults = otsLinks.stream()
                .map(w -> getOtsLinkOtdrLatestResult(w, otdrTopoHolder, latestOTDRByTp)).collect(
                        Collectors.toList());
        return otsLinkOtdrLatestResults;
    }

    private Map<String, OtdrResultRecordDto> buildOtdrPortLatestOTDR(Set<String> otdrPorts) {
        log.debug("get Port latest otdr result by port :{}", otdrPorts);
        List<OtdrResultRecordDto> otdrResults = otdrResultRecordDao.findLatestDirectionOtdrResultsByTpIds(
                new ArrayList<>(otdrPorts));
        Map<String, OtdrResultRecordDto> latestByTpDir = otdrResults.stream()
                .collect(Collectors.toMap(
                        r -> key(r.getTpId(), r.getMonitorDirection()),
                        Function.identity()));
        return latestByTpDir;
    }

    /**
     * get ots link otdr latest result direction is a->z and z->a
     *
     * @param otsWrappedLink
     * @return
     */
    private OtsLinkOtdrLatestResult getOtsLinkOtdrLatestResult(WrappedLink otsWrappedLink,
            OTDRTopoHolder otdrTopoHolder,
            Map<String, OtdrResultRecordDto> latestOtdrResultHolder) {
        Link otsLink = otsWrappedLink.getLink();
        boolean isAligned = otsWrappedLink.getIsAlign();
        log.debug("get ots link latest otdr result,ots link id :{}", otsLink.getLinkId());
        Physical otsLinkPhysical = otsLink.getAugmentation(Link1.class).getPhysical();
        String phyLinkName = otsLinkPhysical.getFriendlyName();
        Provider otsLinkProviderInfo = otsLinkPhysical.getProvider();
        OtsLinkTerminationPointInfo scanEndpoint = otdrTopoHolder.getOtsLinkTpInfo(
                otsLink.getLinkId().getValue());
        if (scanEndpoint == null) {
            scanEndpoint = scanPortHelper.resolveOtsLinkEndpoint(otsLink.getLinkId().getValue());
        }
        String sourceTpId = scanEndpoint.getEdfaSourceTp();
        String destTpId = scanEndpoint.getEdfaDestTp();

        OtdrResultRecordDto sourceZAOtdrLatestRecord = latestOtdrResultHolder.get(
                key(sourceTpId, MonitorDirection.IN.getIntValue()));
        OtdrResultRecordDto sourceAZOtdrLatestRecord = latestOtdrResultHolder.get(
                key(sourceTpId, MonitorDirection.OUT.getIntValue()));
        OtdrResultRecordDto destZAOtdrLatestRecord = latestOtdrResultHolder.get(
                key(destTpId, MonitorDirection.OUT.getIntValue()));
        OtdrResultRecordDto destAZOtdrLatestRecord = latestOtdrResultHolder.get(
                key(destTpId, MonitorDirection.IN.getIntValue()));
        //za latest result record dto
        OtdrResultRecordDto latestZtoARecordDto = getLatestRecordDto(sourceZAOtdrLatestRecord,
                destZAOtdrLatestRecord);
        //az latest result record dto
        OtdrResultRecordDto latestAtoZRecordDto = getLatestRecordDto(sourceAZOtdrLatestRecord,
                destAZOtdrLatestRecord);
        String latestZAScanContent =
                latestZtoARecordDto == null ? null : latestZtoARecordDto.getContent();
        String latestAZScanContent =
                latestAtoZRecordDto == null ? null : latestAtoZRecordDto.getContent();
        OtdrResult ZAOtdrResult =
                StringUtils.isNoneEmpty(latestZAScanContent) ? JSON.parseObject(latestZAScanContent,
                        OtdrResult.class) : null;
        OtdrResult AZOtdrResult =
                StringUtils.isNoneEmpty(latestAZScanContent) ? JSON.parseObject(latestAZScanContent,
                        OtdrResult.class) : null;
        OtsLinkTerminalInfo source = buildTerminalInfoByTp(sourceTpId, otdrTopoHolder);
        OtsLinkTerminalInfo destination = buildTerminalInfoByTp(destTpId, otdrTopoHolder);
        //a to z
        OtsDirectionOtdrResult atoZ = OtsDirectionOtdrResult.builder()
                .contractAttenuationDb(otsLinkProviderInfo.getContractAttenuationAz())
                .contractLength(otsLinkProviderInfo.getDistanceAz())
                .attenuationDb(AZOtdrResult == null ? otsLinkProviderInfo.getContractAttenuationAz()
                        : new BigDecimal(AZOtdrResult.getDetail().getLoss()).setScale(2,
                                RoundingMode.HALF_UP))
                .length(AZOtdrResult == null ? otsLinkProviderInfo.getDistanceAz()
                        : new BigDecimal(AZOtdrResult.getDetail().getDistance()).setScale(2,
                                RoundingMode.HALF_UP))
                .build();

        //z to a
        OtsDirectionOtdrResult ztoA = OtsDirectionOtdrResult.builder()
                .contractAttenuationDb(otsLinkProviderInfo.getContractAttenuationZa())
                .contractLength(otsLinkProviderInfo.getDistanceZa())
                .attenuationDb(ZAOtdrResult == null ? otsLinkProviderInfo.getContractAttenuationZa()
                        : new BigDecimal(ZAOtdrResult.getDetail().getLoss()).setScale(2,
                                RoundingMode.HALF_UP))
                .length(ZAOtdrResult == null ? otsLinkProviderInfo.getDistanceZa()
                        : new BigDecimal(ZAOtdrResult.getDetail().getDistance()).setScale(2,
                                RoundingMode.HALF_UP))
                .build();

        return OtsLinkOtdrLatestResult.builder().linkId(otsLink.getLinkId().getValue())
                .linkName(phyLinkName)
                .source(source)
                .destination(destination)
                .aToz(atoZ)
                .zToa(ztoA)
                .build();
    }

    private OtsLinkTerminalInfo buildTerminalInfoByTp(String terminationPointId,
            OTDRTopoHolder otdrTopoHolder) {
        log.debug("build terminal info by tp Id:{}", terminationPointId);
        String nodeId = PhysicalTpIdNamingRule.getNodeId(terminationPointId);
        String siteId = PhysicalTpIdNamingRule.getSiteId(terminationPointId);
        Node site = otdrTopoHolder.getSiteById(siteId);
        String siteName = site.getAugmentation(Node1.class).getSite().getFriendlyName();
        Node ne = otdrTopoHolder.getPhyNodeById(nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nePhysical = ne.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical();
//        Optional<TerminationPoint> refTpOptional = ne.getTerminationPoint().stream()
//                .filter(terminationPoint -> terminationPoint.getTpId().getValue()
//                        .equals(terminationPointId)).findAny();
        TerminationPoint tp = otdrTopoHolder.getNodeTps(nodeId).get(terminationPointId);
        String tpName = tp == null ? null
                : tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName();
        return OtsLinkTerminalInfo.builder().tpId(terminationPointId)
                .nodeId(ne.getNodeId().getValue())
                .nodeIp(nePhysical.getIp())
                .nodeName(nePhysical.getFriendlyName())
                .siteId(site.getNodeId().getValue())
                .siteName(siteName)
                .tpName(tpName)
                .build();
    }

    private OtdrResultRecordDto getLatestRecordDto(OtdrResultRecordDto sourceOtdrLatestRecord,
            OtdrResultRecordDto destOtdrLatestRecord) {
        // 假设 sourceZA 和 destZA 分别代表从源到目标和从目标到源的OTDR记录
        OtdrResultRecordDto sourceZA = sourceOtdrLatestRecord;
        OtdrResultRecordDto destZA = destOtdrLatestRecord;
        OtdrResultRecordDto latestRecordDto = null;

        if (sourceZA != null) {
            if (destZA != null) {
                // 如果两个记录都有，选择时间更新的记录
                latestRecordDto =
                        sourceZA.getStartTime().compareTo(destZA.getStartTime()) > 0 ? sourceZA
                                : destZA;
            } else {
                // 如果只有 sourceZA 有记录，直接使用它
                latestRecordDto = sourceZA;
            }
        } else if (destZA != null) {
            // 如果只有 destZA 有记录，直接使用它
            latestRecordDto = destZA;
        }
        return latestRecordDto;
    }

    private OtdrBrieflyResult convert2BrieflyOutputResult(OtdrResultRecord otdrResult,
            String linkId,
            Provider provider, Map<String, Node> nodeMap) {

        String neId = PhysicalTpIdNamingRule.getNodeId(otdrResult.getTpId());
        String neName = nodeMap.get(neId).getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical().getFriendlyName();
        String monitorTpName = neName + POUND + otdrResult.getTpName();
        OtdrBrieflyResult result = new OtdrBrieflyResult();
        boolean isSource = PhysicalLinkIdNamingRule.getNodeAId(linkId)
                .equals(otdrResult.getTpId());
//        result.setKey(new ResultKey(BigInteger.valueOf(otdrResult.getId())));
        OtdrMonitorDirection otdrMonitorDirection = OtdrUtils.getMonitorDirection(isSource,
                MonitorDirection.forValue(otdrResult.getMonitorDirection()));
        result.setNodeId(otdrResult.getNeId());
        result.setStartTime(otdrResult.getStartTime().getTime());
        result.setMonitorPort(otdrResult.getTpId());
        result.setMonitorDirection(
                MonitorDirection.forValue(otdrResult.getMonitorDirection()).name());
        result.setMonitorPortName(monitorTpName);
        OtdrScanResultType otdrScanResultType = OtdrScanResultType.forValue(
                otdrResult.getScanResult());
        if (otdrScanResultType != OtdrScanResultType.INPROGRESS) {
            if (provider == null) {
                result.setDistance(String.format("%.2f", otdrResult.getDistance()));
                result.setBaseDistance(String.format("%.2f", otdrResult.getBaseDistance()));
                result.setDeltaDistance(otdrResult.getDeltaDistance() == null ? null
                        : String.format("%.2f", otdrResult.getDeltaDistance()));
                result.setLoss(String.format("%.2f", otdrResult.getLoss()));
                result.setBaseLoss(otdrResult.getBaseLoss() == null ? null
                        : String.format("%.2f", otdrResult.getBaseLoss()));
                result.setDeltaLoss(otdrResult.getDeltaLoss() == null ? null
                        : String.format("%.2f", otdrResult.getDeltaLoss()));
            } else {
                Double baseDistance =
                        otdrMonitorDirection.equals(OtdrMonitorDirection.AZ)
                                ? provider.getDistanceAz()
                                .doubleValue()
                                : provider.getDistanceZa().doubleValue();
                Double deltaDistance = otdrResult.getDistance() - baseDistance;
                Double baseLoss = otdrMonitorDirection.equals(OtdrMonitorDirection.AZ)
                        ? provider.getContractAttenuationAz().doubleValue()
                        : provider.getContractAttenuationZa().doubleValue();
                Double deltaLoss = otdrResult.getLoss() - baseLoss;
                result.setDistance(String.format("%.2f", otdrResult.getDistance()));
                result.setBaseDistance(String.format("%.2f", baseDistance));
                result.setDeltaDistance(String.format("%.2f", deltaDistance));
                result.setLoss(String.format("%.2f", otdrResult.getLoss()));
                result.setBaseLoss(String.format("%.2f", baseLoss));
                result.setDeltaLoss(
                        String.format("%.2f", deltaLoss));
            }
        }
        result.setScanMode(otdrResult.getScanMode());
        result.setTaskId(otdrResult.getId());
        return result;
    }


    private Result conver2OutputResult(OtdrResultRecord otdrResult,
            OtdrBaseBenchmark otdrBaseBenchmark, Map<String, Node> nodeMap) {
        String neId = PhysicalTpIdNamingRule.getNodeId(otdrResult.getTpId());
        String neName = nodeMap.get(neId).getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical().getFriendlyName();
        String monitorTpName = neName + POUND + otdrResult.getTpName();
        ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.setKey(new ResultKey(BigInteger.valueOf(otdrResult.getId())));
        resultBuilder.setNodeId(new NodeId(otdrResult.getNeId()));
        resultBuilder.setStartTime(
                DataTimeConvert.convertToDateAndTime(otdrResult.getStartTime()).getValue());
        resultBuilder.setMonitorPort(otdrResult.getTpId());
        resultBuilder.setMonitorDirection(otdrResult.getMonitorDirectionStr());
        resultBuilder.setMonitorPortName(monitorTpName);
        if (otdrBaseBenchmark == null) {
            resultBuilder.setDistance(String.format("%.2f", otdrResult.getDistance()));
            resultBuilder.setBaseDistance(String.format("%.2f", otdrResult.getBaseDistance()));
            resultBuilder.setDeltaDistance(otdrResult.getDeltaDistance() == null ? null
                    : String.format("%.2f", otdrResult.getDeltaDistance()));
            resultBuilder.setLoss(String.format("%.2f", otdrResult.getLoss()));
            resultBuilder.setBaseLoss(otdrResult.getBaseLoss() == null ? null
                    : String.format("%.2f", otdrResult.getBaseLoss()));
            resultBuilder.setDeltaLoss(otdrResult.getDeltaLoss() == null ? null
                    : String.format("%.2f", otdrResult.getDeltaLoss()));
        } else {
            resultBuilder.setDistance(String.format("%.2f", otdrResult.getDistance()));
            resultBuilder.setBaseDistance(
                    String.format("%.2f", otdrBaseBenchmark.getBaseDistance()));
            resultBuilder.setDeltaDistance(String.format("%.2f",
                    (otdrResult.getDistance() - otdrBaseBenchmark.getBaseDistance())));
            resultBuilder.setLoss(String.format("%.2f", otdrResult.getLoss()));
            resultBuilder.setBaseLoss(String.format("%.2f", otdrBaseBenchmark.getBaseLoss()));
            resultBuilder.setDeltaLoss(
                    String.format("%.2f", otdrResult.getLoss() - otdrBaseBenchmark.getBaseLoss()));
        }
        resultBuilder.setTaskId(otdrResult.getTaskId());
        return resultBuilder.build();

    }

    private String key(String tpId, int direction) {
        return tpId + POUND + direction;
    }
}
