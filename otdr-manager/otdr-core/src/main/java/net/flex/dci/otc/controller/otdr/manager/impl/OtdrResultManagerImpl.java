package net.flex.dci.otc.controller.otdr.manager.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.OTDRQueryParser;
import net.flex.dci.otc.controller.otdr.components.OTDRResult;
import net.flex.dci.otc.controller.otdr.components.OtdrLatestResult;
import net.flex.dci.otc.controller.otdr.components.PhyLinkAttenuation;
import net.flex.dci.otc.controller.otdr.dto.OTDRLinkQueryDto;
import net.flex.dci.otc.controller.otdr.manager.OtdrResultManager;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResult;
import net.flex.dci.otc.controller.otdr.model.OtdrResultOutput;
import net.flex.dci.otc.controller.otdr.model.RouteSegment;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResult;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultOutput;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultPaged;
import net.flex.dci.otc.controller.otdr.model.link.PhyLinkInfo;
import net.flex.dci.otc.controller.otdr.model.link.WrappedLink;
import net.flex.dci.otc.controller.otdr.model.otsLink.GetOmsLinkOtdrLatestResultOutputDto;
import net.flex.dci.otc.controller.otdr.model.otsLink.OmsLinkTotalInfo;
import net.flex.dci.otc.controller.otdr.model.otsLink.OmsOtsLinkOtdrLatestRecord;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsLinkOtdrLatestResult;
import net.flex.dci.otc.controller.otdr.model.otsLink.OtsResults;
import net.flex.dci.otc.controller.otdr.topology.OTDRTopoHolder;
import net.flex.dci.otc.controller.otdr.topology.OTDRTopology;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.mongo.dao.OtdrBaseBenchmarkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.otdr.OtdrBaseBenchmark;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrDetailInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.output.Result;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkRole;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/1 11:40
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrResultManagerImpl implements OtdrResultManager {

    private final OtdrLatestResult otdrLatestResult;

    private final OTDRResult otdrResult;

    private final OTDRQueryParser otdrQueryParser;

    private final OtdrBaseBenchmarkDao otdrBaseBenchmarkDao;

    private final PhyLinkDao phyLinkDao;

    private final PhyLinkAttenuation phyLinkAttenuation;

    private final SiteLinkDao siteLinkDao;

    private final OTDRTopology otdrTopology;


    /**
     * get latest otdr result
     *
     * @param monitorTpId
     * @return
     */
    @Override
    public OtdrResultOutput getLatestOtdrResult(String monitorTpId) {
        return otdrLatestResult.getLatestOtdrResultByMonitorTpId(monitorTpId);
    }

    @Override
    public GetOtdrResultsOutput getOtdrResultsPaged(GetOtdrResultsInput input) {
        log.debug("start to get otdr result paged");
        OTDRLinkQueryDto otdrPagedQueryDto = otdrQueryParser.parsePagedDto(input);

        Page<Result> otdrResultRecordPage = otdrResult.listByQueryItemPaged(
                otdrPagedQueryDto);
        GetOtdrResultsOutputBuilder outputBuilder = new GetOtdrResultsOutputBuilder();

        outputBuilder.setResult(otdrResultRecordPage.toList());
        outputBuilder.setTotalRecords((int) otdrResultRecordPage.getTotalElements());
        return outputBuilder.build();
    }

    @Override
    public String getOtdrResultDetail(GetOtdrDetailInput getOtdrDetailInput) {
        log.debug("get otdr result detail ");
        BigInteger taskId = getOtdrDetailInput.getTaskId();
        OtdrResultRecord otdrDetailResult = otdrResult.getOtdrDetailResultByTaskId(taskId);
        if (otdrDetailResult == null) {
            return null;
        }
        String detailContent = otdrDetailResult.getContent();
        return detailContent;
    }

    @Override
    public LatestOtdrResOutput getLatestOTDRResult(String monitorTpId) {
        log.info("start to get the latest otdr result for the monitor tp ,the tp id is:{}",
                monitorTpId);
        LatestOtdrResult latestOtdrResult = otdrLatestResult.getLatestOtdrResByMonitorTpId(
                monitorTpId);
        LatestOtdrResOutput latestOtdrResOutput = LatestOtdrResOutput.builder()
                .output(latestOtdrResult).build();
        return latestOtdrResOutput;
    }

    @Override
    public void setOtdrBaseBenchmark(String linkId, BigInteger taskId) {
        log.info("set the otdr scan base benchmark ");
        log.debug("base benchmark for link:{},taskId :{}", linkId, taskId);
        Link refPhyLink = phyLinkDao.getPhyLinkById(linkId);
        PhyLinkInfo phyLinkInfo = OtdrUtils.getPhyLinkBrieflyInfo(refPhyLink);
        OtdrResultRecord otdrResultRecord = otdrResult.getOtdrDetailResultByTaskId(taskId);
        Double distance = otdrResultRecord.getDistance();
        Double loss = otdrResultRecord.getLoss();
        String monitorTpId = otdrResultRecord.getTpId();
        Integer direction = otdrResultRecord.getMonitorDirection();
        StartOtdrParameter.MonitorDirection monitorDirection = StartOtdrParameter.MonitorDirection.forValue(
                direction);
        OtdrBaseBenchmark otdrBaseBenchmark = OtdrBaseBenchmark.builder().linkId(linkId)
                .baseDistance(distance)
                .baseLoss(loss)
                .build();
        otdrBaseBenchmarkDao.setTheOtdrBaseBenchmark(otdrBaseBenchmark);
        log.debug("finish to set otdr scan base benchmark");
        phyLinkAttenuation.setBasePhyLinkAttenuationValue(phyLinkInfo, monitorTpId, loss,
                monitorDirection);
    }

    @Override
    public OtdrBrieflyResultOutput getOtdrBrieflyResultsPaged(GetOtdrResultsInput input) {
        log.debug("start to get otdr result paged");
        OTDRLinkQueryDto otdrPagedQueryDto = otdrQueryParser.parsePagedDto(input);

        Page<OtdrBrieflyResult> otdrResultRecordPage = otdrResult.listResultByQueryItemPaged(
                otdrPagedQueryDto);
        OtdrBrieflyResultPaged otdrBrieflyResultPaged = new OtdrBrieflyResultPaged();
        otdrBrieflyResultPaged.setTotalRecords((int) otdrResultRecordPage.getTotalElements());
        otdrBrieflyResultPaged.setResult(otdrResultRecordPage.toList());
        OtdrBrieflyResultOutput otdrBrieflyResultOutput = OtdrBrieflyResultOutput.builder()
                .output(otdrBrieflyResultPaged).build();
        return otdrBrieflyResultOutput;
    }

    @Override
    public GetOmsLinkOtdrLatestResultOutputDto getOMSRefOtsLinkLatestOtdrResult(String omsLinkId,
            LinkRole linkRole) {
        log.debug("get oms link ref ots link latest otdr result,the oms link id :{}", omsLinkId);
        Link omsLink = siteLinkDao.getSiteLinkById(omsLinkId);
        if (omsLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("OMS link: %s is not existed", omsLinkId));
        }
        Site omsLinkPhysical = omsLink.getAugmentation(Link1.class).getSite();
        String omsName = omsLinkPhysical.getFriendlyName();
        log.debug("current oms link :{}", omsName);

        List<Link> otsLinks = getSiteLinkOtsLinkByLinkRole(omsLink, linkRole);
        LinkedList<WrappedLink> sortedWrappedOtsLink = getSiteLinkOtsLinkSorted(
                omsLinkPhysical.getExplictRoute(), otsLinks, linkRole);
        OTDRTopoHolder otdrTopoHolder = otdrTopology.preload(otsLinks);
        List<OtsLinkOtdrLatestResult> otsLinkOtdrLatestResults = otdrResult.getOtsLinksOtdrLatestResult(
                sortedWrappedOtsLink, otdrTopoHolder);
        BigDecimal azRouteLength = BigDecimal.ZERO;
        BigDecimal zaRouteLength = BigDecimal.ZERO;
        BigDecimal contractAzRouteLength = BigDecimal.ZERO;
        BigDecimal contractZaRouteLength = BigDecimal.ZERO;
        for (OtsLinkOtdrLatestResult otsLinkOtdrLatestResult : otsLinkOtdrLatestResults) {
            azRouteLength = azRouteLength.add(otsLinkOtdrLatestResult.getAToz().getLength());
            contractAzRouteLength = contractAzRouteLength.add(otsLinkOtdrLatestResult.getAToz()
                    .getContractLength());
            zaRouteLength = zaRouteLength.add(otsLinkOtdrLatestResult.getZToa().getLength());
            contractZaRouteLength = contractZaRouteLength.add(otsLinkOtdrLatestResult.getZToa()
                    .getContractLength());
        }
//        List<OtsLinkOtdrLatestResult> sortedOtsLinkOtdrLatestResult = getSortedOtsLinkOtdrLatestResult(
//                otsLinkOtdrLatestResults, sortedOtsLinkIds);
        OmsLinkTotalInfo omsOtsLinkTotalInfo = OmsLinkTotalInfo.builder()
                .aZRouteLength(azRouteLength)
                .zARouteLength(zaRouteLength)
                .contractAzRouteLength(contractAzRouteLength)
                .contractZaRouteLength(contractZaRouteLength)
                .omsLinkName(omsName)
                .omsLinkId(omsLinkId)
                .build();
        OtsResults otsResults = OtsResults.builder()
                .otsLinks(otsLinkOtdrLatestResults)
                .build();
        OmsOtsLinkOtdrLatestRecord omsOtsLinkOtdrLatestRecord = OmsOtsLinkOtdrLatestRecord.builder()
                .otsResults(otsResults)
                .omsLinkTotalInfo(omsOtsLinkTotalInfo)
                .build();
        GetOmsLinkOtdrLatestResultOutputDto getOmsLinkOtdrLatestResultOutputDto = GetOmsLinkOtdrLatestResultOutputDto.builder()
                .output(omsOtsLinkOtdrLatestRecord)
                .build();
        return getOmsLinkOtdrLatestResultOutputDto;
    }

    private List<Link> getSiteLinkOtsLinkByLinkRole(Link siteLink, LinkRole linkRole) {
        log.debug("get current site link link role:{} ots linkId :{}", siteLink.getLinkId(),
                linkRole);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        Class<? extends ProtectionType> protectionType = siteLinkAttr.getProtectionType();
        List<String> otsLinkIds = new ArrayList<>();
        Route siteLinkRoute = siteLinkAttr.getExplictRoute().getRoute().get(0);
        if (protectionType.isAssignableFrom(ProtectionUnprotected.class)) {
            otsLinkIds = getUnprotectSiteLinkOtsLinkIds(siteLinkRoute, linkRole);
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To1.class)) {
            otsLinkIds = getProtectionBidr1To1OtsLinkIds(siteLinkRoute, linkRole);
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To2.class)) {
            otsLinkIds = getProtectionBidr1To2OtsLinkIds(siteLinkRoute, linkRole);
        }
        List<Link> otsLinks = phyLinkDao.getAllPhyLinksByIds(otsLinkIds);
        return otsLinks;
    }

    private List<String> getProtectionBidr1To2OtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        log.debug("get bid1to1 siteLink ots link link role:{}", linkRole);
        List<PathRouteObject> routeObjects = getExplicitRouteObjectByLinkRole(siteLinkRoute,
                linkRole).getPathRouteObject();
        List<String> otsLinkIds = getPathRouteOtsLinks(routeObjects);
        return otsLinkIds;
    }


    private List<String> getProtectionBidr1To1OtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        if (linkRole == LinkRole.Tertiary) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "current site link route is 1+1 protection do not support tertiary");
        }
        List<PathRouteObject> routeObjects = getExplicitRouteObjectByLinkRole(siteLinkRoute,
                linkRole).getPathRouteObject();
        List<String> otsLinkIds = getPathRouteOtsLinks(routeObjects);
        return otsLinkIds;
    }

    private List<String> getUnprotectSiteLinkOtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        if (linkRole == LinkRole.Secondary || linkRole == LinkRole.Tertiary) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "current site link route is unprotection do not support secondary and tertiary");
        }
        List<PathRouteObject> routeObjects = getExplicitRouteObjectByLinkRole(siteLinkRoute,
                linkRole).getPathRouteObject();
        List<String> otsLinkIds = getPathRouteOtsLinks(routeObjects);
        return otsLinkIds;

    }

    private List<String> getPathRouteOtsLinks(List<PathRouteObject> routeObjects) {
        List<String> otsLinkIds = new ArrayList<>();
        for (PathRouteObject routeObject : routeObjects) {
            ResourceType resourceType = routeObject.getResourceType();
            if (resourceType instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .tunnel.types.rev180515.resource.type.resource.type.Link) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link linkResource = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) resourceType;
                String linkId = linkResource.getLinkHop().getLinkRef().getValue();
                if (PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    otsLinkIds.add(linkId);
                }
            }
        }
        return otsLinkIds;
    }


    private List<OtsLinkOtdrLatestResult> getSortedOtsLinkOtdrLatestResult(
            List<OtsLinkOtdrLatestResult> otsLinkOtdrLatestResults, List<String> sortedOtsLinkIds) {
        log.debug("sort amplifier info by sorted link ids:{}", sortedOtsLinkIds);
        Map<String, OtsLinkOtdrLatestResult> infoById = otsLinkOtdrLatestResults.stream()
                .collect(Collectors.toMap(
                        OtsLinkOtdrLatestResult::getLinkId,
                        Function.identity()
                ));

        List<OtsLinkOtdrLatestResult> sortedOtdrInfos = sortedOtsLinkIds.stream()
                .map(infoById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return sortedOtdrInfos;
    }

    private LinkedList<WrappedLink> getSiteLinkOtsLinkSorted(ExplictRoute explictRoute,
            List<Link> otsLinks, LinkRole linkRole) {
        log.debug("get ots link sorted by site link route");
        List<RouteSegment> routeSegments = extractRouteSegments(explictRoute, linkRole);
        Map<String, Link> linkById = otsLinks.stream()
                .collect(
                        Collectors.toMap(link -> link.getLinkId().getValue(), Function.identity()));
        LinkedList<WrappedLink> result = new LinkedList<>();

        for (RouteSegment seg : routeSegments) {
            Link link = linkById.get(
                    seg.getLinkId());
            if (link == null) {
                log.warn("route link [{}] cannot found in the link list", seg.getLinkId());
                continue;
            }

            boolean sameDirection =
                    link.getSource().getSourceTp().getValue().equals(seg.getSourceTp()) &&
                            link.getDestination().getDestTp().getValue().equals(seg.getDestTp());

            // 如果是反向，则 sameDirection=false，可以在包装类中存方向标记
            result.add(WrappedLink.builder().link(link).isAlign(sameDirection).build());
        }

        return result;
    }

    /**
     * 提取路由段信息：srcTp、linkId、dstTp
     */
    private List<RouteSegment> extractRouteSegments(ExplictRoute explictRoute, LinkRole linkRole) {
        List<RouteSegment> segments = new ArrayList<>();
        Route route = explictRoute.getRoute().get(0);
        ExplicitRouteObjects ero = getExplicitRouteObjectByLinkRole(route, linkRole);
        List<PathRouteObject> objs = ero.getPathRouteObject();

        String prevTp = null;
        for (PathRouteObject obj : objs) {
            ResourceType rt = obj.getResourceType();
            if (rt instanceof Tp) {
                prevTp = ((Tp) rt).getTpHop().getTpRef().getValue();
            } else if (rt instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .tunnel.types.rev180515.resource.type.resource.type.Link) {

                String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                        .tunnel.types.rev180515.resource.type.resource.type.Link) rt)
                        .getLinkHop().getLinkRef().getValue();

                if (!PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    continue;
                }

                // 找下一个 TP
                int idx = objs.indexOf(obj);
                String nextTp = null;
                if (idx + 1 < objs.size()) {
                    ResourceType nextRt = objs.get(idx + 1).getResourceType();
                    if (nextRt instanceof Tp) {
                        nextTp = ((Tp) nextRt).getTpHop().getTpRef().getValue();
                    }
                }

                if (prevTp != null && nextTp != null) {
                    segments.add(
                            RouteSegment.builder().sourceTp(prevTp).linkId(linkId).destTp(nextTp)
                                    .build());
                }
                prevTp = nextTp; // 更新
            }
        }
        return segments;
    }

    private ExplicitRouteObjects getExplicitRouteObjectByLinkRole(Route route, LinkRole linkRole) {
        switch (linkRole) {
            case Primary:
                return route.getPrimary().getExplicitRouteObjects().get(0);
            case Secondary:
                return route.getSecondary().getExplicitRouteObjects().get(0);
            case Tertiary:
                return route.getThird().get(0).getExplicitRouteObjects().get(0);
            default:
                throw new IllegalArgumentException("Unsupported link role: " + linkRole);
        }
    }


}
