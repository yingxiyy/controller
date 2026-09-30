package net.flex.dci.otc.controller.status.core.calculator.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.EquipmentAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.SiteNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.ViewNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.OchLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.PhyLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.SiteLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.TunnelAlarmStateCalculator;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/4 11:41
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmStateCalculator implements IAlarmStateCalculator<AlarmStateResult, Node> {

    private final PhyLinkAlarmStateCalculator phyLinkAlarmStateCalculator;

    private final SiteLinkAlarmStateCalculator siteLinkAlarmStateCalculator;

    private final TunnelAlarmStateCalculator tunnelAlarmStateCalculator;

    private final ViewNodeAlarmStateCalculator viewNodeAlarmStateCalculator;

    private final SiteNodeAlarmStateCalculator siteNodeAlarmStateCalculator;

    private final EquipmentAlarmStateCalculator equipmentAlarmStateCalculator;

    private final OchLinkAlarmStateCalculator ochLinkAlarmStateCalculator;

    @Override
    public AlarmStateResult calculate(Node phyNode) {
        log.info("start to handle the alarm state calculator for phyNode neId:{}",
                phyNode.getNodeId().getValue());
//        EquipmentsAlarmState equipmentsAlarmState = equipmentAlarmStateCalculator.calculate(
//                phyNode);
        //ViewNode calculate state
//        ViewNodeAlarmState viewNodeAlarmState = viewNodeAlarmStateCalculator.calculate(phyNode);
//        //siteNodeAlarmState
//        SiteNodeAlarmState siteNodeAlarmState = siteNodeAlarmStateCalculator.calculate(phyNode);
//        //tunnel alarm state change
//
//        //phyLinkAlarmState
//        PhyLinksAlarmState phyLinkAlarmState = phyLinkAlarmStateCalculator.calculate(phyNode);
//        //och link alarm change
//        SiteLinksAlarmState siteLinksAlarmState = siteLinkAlarmStateCalculator.calculate(phyNode);
//        //och link alarm change
//        OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(phyNode);
//        TunnelAlarmState tunnelAlarmState = tunnelAlarmStateCalculator.calculate(phyNode);
//        return AlarmStateResult.builder()
//                .viewNodeAlarmState(viewNodeAlarmState)
//                .siteNodeAlarmState(siteNodeAlarmState)
//                .tunnelAlarmState(tunnelAlarmState)
//                .phyLinkAlarmState(phyLinkAlarmState)
//                .siteLinkAlarmState(siteLinksAlarmState)
//                .ochLinkAlarmState(ochLinksAlarmState)
//                .build();
        return null;
    }

    @Override
    public AlarmStateResult calculate(String id) {
        log.info("start to handle the alarm state calculator for phyNode neId:{}", id);
        String neId = StatusUtil.getPhyNeId(id);
        //ViewNode calculate state
        ViewNodeAlarmState viewNodeAlarmState = viewNodeAlarmStateCalculator.calculate(neId);
        return AlarmStateResult.builder().viewNodeAlarmState(viewNodeAlarmState).build();
    }


}
