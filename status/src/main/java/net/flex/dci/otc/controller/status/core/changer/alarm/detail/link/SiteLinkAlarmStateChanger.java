package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState.SiteLinkAlarmState;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.batch.SiteLinkAlarmStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/5 19:08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteLinkAlarmStateChanger extends AbstractLinkAlarmStateChanger<SiteLinksAlarmState> {

    private final SiteLinkDao siteLinkDao;

    @Override
    public void changeState(SiteLinksAlarmState state) {
        log.debug("update site links alarm state ,change state is {}", state);
        List<SiteLinkAlarmState> siteLinkAlarmStateList = state.getSiteLinkAlarmStates();
        List<SiteLinkAlarmStatus> updateSiteLinkAlarmStatusList = siteLinkAlarmStateList.stream()
                .map(siteLinkAlarmState -> SiteLinkAlarmStatus.builder()
                        .alarmSeverity(siteLinkAlarmState.getAlarmSeverity())
                        .linkId(siteLinkAlarmState.getSiteLinkId())
                        .build()
                ).collect(
                        Collectors.toList());
        siteLinkDao.bulkUpdateSiteLinkAlarmState(updateSiteLinkAlarmStatusList);
        log.debug("end to update the site link alarm state!");
        List<String> refSiteLinkIds = siteLinkAlarmStateList.stream()
                .map(SiteLinkAlarmState::getSiteLinkId).collect(
                        Collectors.toList());
        viewLinkAlarmStateEvaluate.evaluateAlarmState(refSiteLinkIds, LinkType.SITE_LINK);
    }

//    private void siteLinkAlarmChanger(SiteLinkAlarmState siteLinkAlarmState) {
//        String siteLinkId = siteLinkAlarmState.getSiteLinkId();
//        AlarmSeverity alarmSeverity = siteLinkAlarmState.getAlarmSeverity();
//        log.debug("update site link ,site link Id:{},alarm severity :{}", siteLinkId,
//                alarmSeverity);
////        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
//        siteLinkDao.updateSiteLinkAlarmState(siteLinkId, alarmSeverity);
//
////        Site siteLinkPhysical = siteLink.getAugmentation(Link1.class).getSite();
////        SiteBuilder siteBuilder = new SiteBuilder(siteLinkPhysical);
////        siteBuilder.setAlarmState(alarmSeverity);
////
////        LinkBuilder linkBuilder = new LinkBuilder(siteLink);
////        Link1Builder link1Builder = new Link1Builder(siteLink.getAugmentation(Link1.class));
////        link1Builder.setSite(siteBuilder.build());
////        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
////        siteLinkDao.saveSiteLink(linkBuilder.build());
//    }
}
