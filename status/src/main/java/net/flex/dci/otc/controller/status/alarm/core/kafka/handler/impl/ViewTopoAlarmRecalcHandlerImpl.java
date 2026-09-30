package net.flex.dci.otc.controller.status.alarm.core.kafka.handler.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;
import net.flex.dci.otc.controller.status.alarm.core.kafka.handler.ViewTopoAlarmRecalcHandler;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.ViewNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.ViewNodeAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.ViewLinkAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.core.evaluate.ViewLinkAlarmStateEvaluate;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/2/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTopoAlarmRecalcHandlerImpl implements ViewTopoAlarmRecalcHandler {


    private final ViewNodeAlarmStateCalculator viewNodeAlarmStateCalculator;

    private final ViewLinkDao viewLinkDao;

    private final ViewLinkAlarmStateEvaluate viewLinkAlarmStateEvaluate;

    private final ViewNodeAlarmStateChanger viewNodeAlarmStateChanger;

    private final ViewLinkAlarmStateChanger viewLinkAlarmStateChanger;


    @Override
    public void recalculateViewTopo(ViewTopoAlarmRecalcMsg topoAlarmRecalcMsg) {
        log.info("recalculate view topo:{}", topoAlarmRecalcMsg);
        List<String> viewNodeAlarmRecalcIds = topoAlarmRecalcMsg.getViewNodeIds();
        List<String> viewLinkAlarmRecalcIds = topoAlarmRecalcMsg.getViewLinkIds();

        //recalculate view node
        List<ViewNodeAlarmState> viewNodeAlarmStates = recalculateViewNodeAlarm(
                viewNodeAlarmRecalcIds);
        //recalculate view link
        List<ViewLinkAlarmState> viewLinkAlarmStates = recalculateViewLinkAlarm(
                viewLinkAlarmRecalcIds);
        updateViewTopoAlarmState(viewNodeAlarmStates, viewLinkAlarmStates);
    }

    private void updateViewTopoAlarmState(List<ViewNodeAlarmState> viewNodeAlarmStates,
            List<ViewLinkAlarmState> updateViewLinkAlarmStates) {
        log.debug("update view node alarm state:{} view Link alarm state:{}", viewNodeAlarmStates,
                updateViewLinkAlarmStates);
        viewNodeAlarmStateChanger.changeBatchState(viewNodeAlarmStates);
        viewLinkAlarmStateChanger.changeState(
                ViewLinksAlarmState.builder().viewLinkAlarmStates(updateViewLinkAlarmStates)
                        .build());
    }

    private List<ViewLinkAlarmState> recalculateViewLinkAlarm(List<String> viewLinkAlarmRecalcIds) {
        log.info("recalculate view link alarm change view linkIds:{}", viewLinkAlarmRecalcIds);
        if (CollectionUtils.isEmpty(viewLinkAlarmRecalcIds)) {
            log.warn("recalculate view link is empty,skip");
            return new ArrayList<>();
        }
        List<Link> viewLinks = viewLinkDao.listAllViewLinkByLinkIds(viewLinkAlarmRecalcIds);
        Map<LinkType, List<Link>> linkTypeListMap = new HashMap<>();
        for (Link viewLink : viewLinks) {
            View viewLinkPhysical = viewLink.getAugmentation(
                    Link1.class).getView();
            ViewLinkType viewLinkType = viewLinkPhysical.getLevel();
            LinkType linkType = LinkType.fromViewLinkType(viewLinkType);
            linkTypeListMap.computeIfAbsent(linkType, k -> new ArrayList<>()).add(viewLink);
        }
        List<ViewLinkAlarmState> viewLinkAlarmStates = new ArrayList<>();
        for (Map.Entry<LinkType, List<Link>> entry : linkTypeListMap.entrySet()) {
            LinkType linkType = entry.getKey();
            List<Link> links = entry.getValue();
            List<ViewLinkAlarmState> viewLinkAlarmStateList = viewLinkAlarmStateEvaluate.calculateViewLinksAlarmState(
                    links, linkType);
            viewLinkAlarmStates.addAll(viewLinkAlarmStateList);
        }
        return viewLinkAlarmStates;
    }

    private List<ViewNodeAlarmState> recalculateViewNodeAlarm(List<String> viewNodeAlarmRecalcIds) {
        log.info("recalculate view node alarm change view node ids:{}", viewNodeAlarmRecalcIds);
        if (CollectionUtils.isEmpty(viewNodeAlarmRecalcIds)) {
            log.warn("recalculate view node is empty,skip");
            return new ArrayList<>();
        }
        List<ViewNodeAlarmState> viewNodeAlarmStates = viewNodeAlarmStateCalculator.calculateViewNodeAlarms(
                viewNodeAlarmRecalcIds);
        return viewNodeAlarmStates;
    }


}
