package net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState.OchLinkAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
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
public class OchLinkAlarmStateCalculator extends
        AbstractLinkAlarmStateCalculator<OchLinksAlarmState, List<LinkStateDto>> {

    private final AlarmDaoService alarmDaoService;


    @Override
    public OchLinksAlarmState calculate(List<LinkStateDto> links) {
        log.debug("start to calculate the ref och links alarm state");
        Set<String> allPhyLinkIds = new HashSet<>();
        Set<String> allSiteLinkIds = new HashSet<>();
        for (LinkStateDto ochLink : links) {
            for (String supportLink : ochLink.getSupportingLink()) {
                if (StatusUtil.isSiteLinkId(supportLink)) {
                    allSiteLinkIds.add(supportLink);
                } else {
                    allPhyLinkIds.add(supportLink);
                }
            }
        }
        Map<String, LinkStateDto> phyLinkMap = connectionCacheManager.batchGetPhyLinksByIds(
                new ArrayList<>(allPhyLinkIds));
        Map<String, LinkStateDto> siteLinkMap = connectionCacheManager.batchGetSiteLinksByIds(
                new ArrayList<>(allSiteLinkIds));
//        List<OchLinkAlarmState> ochLinkAlarmStates = links.stream()
//                .map(this::calculateOchLinkAlarmState).collect(
//                        Collectors.toList());
        List<OchLinkAlarmState> ochLinkAlarmStates = links.stream()
                .map(ochLink -> calculateOchLinkAlarmState(ochLink, phyLinkMap, siteLinkMap))
                .collect(Collectors.toList());
        return OchLinksAlarmState.builder().ochLinkAlarmStates(ochLinkAlarmStates).build();
    }

    @Override
    public OchLinksAlarmState calculate(String id) {
        return null;
    }


    /**
     * calculate the och link alarm state to support ochp
     *
     * @param ochLink
     * @param phyLinkMap
     * @param siteLinkMap
     * @return
     */
    public OchLinkAlarmState calculateOchLinkAlarmState(LinkStateDto ochLink,
            Map<String, LinkStateDto> phyLinkMap,
            Map<String, LinkStateDto> siteLinkMap) {
        String ochLinkId = ochLink.getId();
        log.debug("start to calculate the och link alarm state,och link id is :{}", ochLinkId);

        List<String> supportLinks = ochLink.getSupportingLink();
        List<String> phyLinkIds = new ArrayList<>();
        List<String> siteLinkIds = new ArrayList<>();
        for (String supportingLink : supportLinks) {
            if (StatusUtil.isSiteLinkId(supportingLink)) {
                siteLinkIds.add(supportingLink);
            } else {
                phyLinkIds.add(supportingLink);
            }
        }

        AlarmSeverity phyLinkAlarmSeverity = getRefPhyLinksSeverity(phyLinkIds, phyLinkMap);
        AlarmSeverity siteLinkAlarmSeverity = getRefSiteLinksSeverity(siteLinkIds, siteLinkMap);
        AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmSeverity(phyLinkAlarmSeverity,
                siteLinkAlarmSeverity);

        return OchLinkAlarmState.builder().ochLinkId(ochLinkId).alarmSeverity(alarmSeverity)
                .build();
    }

    public OchLinkAlarmState calculateLinkAlarmState(LinkStateDto link, List<String> refLinkIds,
            AlarmSeverity alarmSeverity) {
        String ochLinkId = link.getId();
        log.debug(
                "start to calculate och link alarm state,the och link id is:{} ref phyLink id is:{}",
                ochLinkId, refLinkIds);
//        boolean isProtected = linkHelper.isOchLinkInProtectedMode(link);
        AlarmSeverity updateAlarmSeverity = link.getAlarmSeverity();
//        if (!isProtected) {
        updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(updateAlarmSeverity,
                alarmSeverity);
//        } else {
//            ProtectedLinkDto linkDetailInfo = linkHelper.getOCHPOchWorkModeDetailInfo(link);
//            updateAlarmSeverity = calculateProtectLinkAlarmSeverity(linkDetailInfo, refLinkIds,
//                    updateAlarmSeverity, alarmSeverity);
//        }

        return OchLinkAlarmState.builder().alarmSeverity(updateAlarmSeverity).ochLinkId(ochLinkId)
                .build();
    }

    /**
     * calculate och alarm severity ,the och is not ochp
     *
     * @param ochLink
     * @return
     */
//    private OchLinkAlarmState calculateOchAlarmSeverityUnProtected(Link ochLink) {
//        String ochLinkId = ochLink.getLinkId().getValue();
//        log.debug("calculate the unprotected mode och alarm state ,the och link id is:{}",
//                ochLinkId);
//        List<SupportingLink> supportLinks = ochLink.getSupportingLink();
//        List<String> phyLinkIds = new ArrayList<>();
//        List<String> siteLinkIds = new ArrayList<>();
//        for (SupportingLink supportingLink : supportLinks) {
//            String linkRef = supportingLink.getLinkRef().getValue();
//            if (StatusUtil.isSiteLinkId(linkRef)) {
//                siteLinkIds.add(linkRef);
//            } else {
//                phyLinkIds.add(linkRef);
//            }
//        }
//        AlarmSeverity phyLinkAlarmSeverity = getRefPhyLinksSeverity(phyLinkIds, phyLinkMap);
//        AlarmSeverity siteLinkAlarmSeverity = getRefSiteLinksSeverity(siteLinkIds, siteLinkMap);
//        AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmSeverity(phyLinkAlarmSeverity,
//                siteLinkAlarmSeverity);
//        return OchLinkAlarmState.builder().ochLinkId(ochLinkId).alarmSeverity(alarmSeverity)
//                .build();
//    }

    /**
     * calculate the protected  och alarm severity
     *
     * @param ochLink
     * @return
     */
    private OchLinkAlarmState calculateOchAlarmSeverityProtected(Link ochLink) {
        String ochLinkId = ochLink.getLinkId().getValue();
        log.debug("calculate the protected och alarm severity,the och linkId is:{}", ochLinkId);
        ProtectedLinkDto protectedLinkDto = linkHelper.getOCHPOchWorkModeDetailInfo(ochLink);
        AlarmSeverity alarmSeverity = calculateOchpAlarmState(protectedLinkDto);
        return OchLinkAlarmState.builder().ochLinkId(ochLinkId)
                .alarmSeverity(alarmSeverity).build();
    }

    private AlarmSeverity calculateOchpAlarmState(ProtectedLinkDto protectedLinkDto) {
        log.debug("start to calculate och with protection mode :{}", protectedLinkDto);
        AlarmSeverity alarmSeverity = calculateProtectAlarmSeverity(protectedLinkDto);
        return alarmSeverity;
    }


}
