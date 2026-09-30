package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState.PhyLinkAlarmState;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/5 0:19
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkAlarmStateChanger extends AbstractLinkAlarmStateChanger<PhyLinksAlarmState> {

    private final PhyLinkDao phyLinkDao;

    @Override
    public void changeState(PhyLinksAlarmState state) {
        log.debug("update phy link alarm state:{}", state);
        List<PhyLinkAlarmState> alarmStates = state.getPhyLinksAlarmStates();
        if (alarmStates == null || alarmStates.isEmpty()) {
            log.debug("there have no phy link update");
            return;
        }
//        alarmStates.forEach(this::updatePhyLinkAlarmState);
        List<net.flex.dci.otc.mongo.dto.batch.PhyLinkAlarmState> updateAlarmStates = alarmStates.stream()
                .map(phyLinkAlarmState -> net.flex.dci.otc.mongo.dto.batch.PhyLinkAlarmState.builder()
                        .linkId(phyLinkAlarmState.getPhyLinkId())
                        .alarmSeverity(phyLinkAlarmState.getAlarmSeverity())
                        .build())
                .collect(Collectors.toList());
        phyLinkDao.bulkUpdateLinkAlarmState(updateAlarmStates);
        List<String> refLinkIds = alarmStates.stream()
                .map(PhyLinkAlarmState::getPhyLinkId).collect(
                        Collectors.toList());
        viewLinkAlarmStateEvaluate.evaluateAlarmState(refLinkIds, LinkType.PHYSICAL_LINK);
    }

//    private void updatePhyLinkAlarmState(PhyLinkAlarmState phyLinkAlarmState) {
//        String phyLinkId = phyLinkAlarmState.getPhyLinkId();
//        AlarmSeverity alarmSeverity = phyLinkAlarmState.getAlarmSeverity();
//        phyLinkDao.updateLinkAlarmState(phyLinkId, alarmSeverity);
//        Link phyLink = phyLinkDao.getPhyLinkById(phyLinkId);
//        LinkBuilder linkBuilder = new LinkBuilder(phyLink);
//        PhysicalBuilder physicalBuilder = new PhysicalBuilder(
//                phyLink.getAugmentation(Link1.class).getPhysical());
//        physicalBuilder.setAlarmState(alarmSeverity);
//        linkBuilder.addAugmentation(Link1.class,
//                new Link1Builder(phyLink.getAugmentation(Link1.class)).setPhysical(
//                        physicalBuilder.build()).build());
//        phyLinkDao.savePhyLink(linkBuilder.build());
//    }
}
