package net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState.SiteLinkAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 17:14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkAlarmStateCalculator extends
        AbstractLinkAlarmStateCalculator<SiteLinksAlarmState, List<LinkStateDto>> {


    @Override
    public SiteLinksAlarmState calculate(List<LinkStateDto> siteLinks) {

        List<SiteLinksAlarmState.SiteLinkAlarmState> siteLinkAlarmStates = calculateSiteLinksAlarmState(
                siteLinks);
        return SiteLinksAlarmState.builder().siteLinkAlarmStates(siteLinkAlarmStates).build();
    }

    @Override
    public SiteLinksAlarmState calculate(String id) {
        return null;
    }

    public SiteLinkAlarmState calculateLinkAlarmState(LinkStateDto link, List<String> refLinkIds,
            AlarmSeverity alarmSeverity) {
        log.debug("start to calculate link alarm state");
        String siteLinkId = link.getId();
        AlarmSeverity currentAlarmSeverity = link.getAlarmSeverity();
//        Boolean isProtectedMode = linkHelper.isSiteLinkInProtectedMode(link);
        SiteLinkAlarmState siteLinkAlarmState = null;
//        if (!isProtectedMode) {
        AlarmSeverity updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(
                currentAlarmSeverity, alarmSeverity);
        siteLinkAlarmState = SiteLinkAlarmState.builder().siteLinkId(siteLinkId)
                .alarmSeverity(updateAlarmSeverity).build();
//        } else {
//            ProtectedLinkDto siteLinkDetailInfo = linkHelper.getProtectedSiteLinkDetailInfo(link);
//            AlarmSeverity updateAlarmSeverity = calculateProtectLinkAlarmSeverity(
//                    siteLinkDetailInfo, refLinkIds, currentAlarmSeverity, alarmSeverity);
//            siteLinkAlarmState = SiteLinkAlarmState.builder().siteLinkId(siteLinkId)
//                    .alarmSeverity(updateAlarmSeverity).build();
//        }
        return siteLinkAlarmState;
    }


    private List<SiteLinkAlarmState> calculateSiteLinksAlarmState(List<LinkStateDto> siteLinks) {
        Set<String> allPhyLinkIds = new HashSet<>();
        for (LinkStateDto siteLink : siteLinks) {
            allPhyLinkIds.addAll(siteLink.getSupportingLink());
        }

        Map<String, LinkStateDto> phyLinkMap = connectionCacheManager.batchGetPhyLinksByIds(
                new ArrayList<>(allPhyLinkIds));
        return siteLinks.stream()
                .map(siteLink -> calculateSiteLinkAlarmState(siteLink, phyLinkMap))
                .collect(Collectors.toList());
    }

    private SiteLinkAlarmState calculateSiteLinkAlarmState(LinkStateDto siteLink,
            Map<String, LinkStateDto> phyLinkMap) {
        String linkId = siteLink.getId();
        log.debug("start calculate the site link alarm state,link id is :{}", linkId);
//        Boolean isProtectedMode = linkHelper.isSiteLinkInProtectedMode(siteLink);
        AlarmSeverity alarmSeverity = AlarmSeverity.Unknown;
////        if (isProtectedMode) {
//        alarmSeverity = calculateSeverityProtectMode(siteLink);
//        } else {
        alarmSeverity = calculateSeverityUnprotectMode(siteLink, phyLinkMap);
//        }
        return SiteLinkAlarmState.builder().alarmSeverity(alarmSeverity).siteLinkId(linkId).build();
    }

    /**
     * calculate unprotected mode
     *
     * @param siteLink
     * @return
     */
    private AlarmSeverity calculateSeverityUnprotectMode(LinkStateDto siteLink,
            Map<String, LinkStateDto> phyLinkMap) {
        List<String> phyLinkIds = siteLink.getSupportingLink();
        List<AlarmSeverity> alarmSeverities = phyLinkIds.stream()
                .map(phyLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getAlarmSeverity)
                .collect(Collectors.toList());
        log.debug("current alarm severities is :{}", alarmSeverities);
        return StatusUtil.calculateAlarmStateByList(alarmSeverities);
    }

    /**
     * calculate unprotected mode
     *
     * @param siteLink
     * @return
     */
    private AlarmSeverity calculateSeverityProtectMode(Link siteLink) {
        ProtectedLinkDto protectedSiteLinkDto = linkHelper.getProtectedSiteLinkDetailInfo(
                siteLink);
        AlarmSeverity alarmSeverity = calculateProtectAlarmSeverity(protectedSiteLinkDto);
//        ProtectedSiteLinkWorkModel workModel = protectedSiteLinkDto.getWorkModel();
//        String siteLinkId = siteLink.getLinkId().getValue();
//        log.debug("calculate the protected mode alarm state for site link,site link id:{}",
//                siteLinkId);
//        Site site = siteLink.getAugmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
//                .getSite();
//        Route route = site.getExplictRoute().getRoute().get(0);
//        Primary primary = route.getPrimary();
//        Secondary secondary = route.getSecondary();
//        if (protectedSiteLinkDto.getWorkModel().equals(ProtectedSiteLinkWorkModel.UNKNOWN)) {
//            return AlarmSeverity.Unknown;
//        }
//        Set<String> primaryTps = getRefTpsByRoute(primary.getExplicitRouteObjects());
//        Set<String> secondaryTps = getRefTpsByRoute(secondary.getExplicitRouteObjects());
//        Set<String> sameTps = getBothPrimaryAndSecondaryTps(primary.getExplicitRouteObjects(),
//                protectedSiteLinkDto.getSwitchPortId());
//        secondaryTps.addAll(sameTps);
//
//        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
//        if (workModel.equals(ProtectedSiteLinkWorkModel.PRIMARY)) {
//            alarmSeverity = calculateAlarmSeverityByTps(primaryTps);
//        } else if (workModel.equals(ProtectedSiteLinkWorkModel.SECONDARY)) {
//            alarmSeverity = calculateAlarmSeverityByTps(secondaryTps);
//        } else if (workModel.equals(ProtectedSiteLinkWorkModel.MIX_TYPE)) {
//            Set<String> allTps = StatusUtil.getUnionSetByGuava(primaryTps, secondaryTps);
//            alarmSeverity = calculateAlarmSeverityByTps(allTps);
//        }
//        return alarmSeverity;
        return alarmSeverity;
    }

//    private AlarmSeverity calculateAlarmSeverityByTps(Set<String> tps) {
//        List<String> tpIds = new ArrayList<>(tps);
//        List<AlarmSeverity> alarmSeverities = alarmDaoService.getAlarmSeverityAffectLink(
//                alarmConfigurationProperties.affectLinkKeywordList(), tpIds);
//        return StatusUtil.calculateAlarmStateByList(alarmSeverities);
//    }
//
//    private Set<String> getBothPrimaryAndSecondaryTps(List<ExplicitRouteObjects> primaryRoutes,
//            List<String> switchPortId) {
//        log.debug("get both primary and secondary phy link ids");
//        Set<String> result = new HashSet<>();
//        List<PathRouteObject> proList = primaryRoutes.get(0)
//                .getPathRouteObject();
//        linkHelper.sortPath(proList);
//        if (!proList.isEmpty()) {
//            for (PathRouteObject pro : proList) {
//                if (pro.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) {
//                    String tpId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) pro
//                            .getResourceType()).getTpHop().getTpRef().getValue();
//                    if (!switchPortId.contains(tpId)) {
//                        result.add(tpId);
//                    }
//
//                }
//            }
//        }
//        return result;
//    }
//
//
//    private Set<String> getRefTpsByRoute(List<ExplicitRouteObjects> explicitRouteObjects) {
//        log.debug("get ref nml key from the explicit route object");
//        Set<String> nmlKeys = new HashSet<>();
//        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
//            List<PathRouteObject> routeObjects = explicitRouteObject.getPathRouteObject();
//            routeObjects.forEach(pathRouteObject -> {
//                if (pathRouteObject.getResourceType() instanceof Tp) {
//                    String tpId = ((Tp) pathRouteObject
//                            .getResourceType()).getTpHop().getPhyTp().getTpId().getValue();
//                    nmlKeys.add(tpId);
//                }
//            });
//        }
//        return nmlKeys;
//    }


}
