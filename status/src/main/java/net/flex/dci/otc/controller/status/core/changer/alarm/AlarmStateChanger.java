package net.flex.dci.otc.controller.status.core.changer.alarm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.RackAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.SiteNodeAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.ViewNodeAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.OchLinkAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.PhyLinkAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.SiteLinkAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.TunnelAlarmStateChanger;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.RackAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteNodeAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/4 14:27
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmStateChanger implements IStateChanger<AlarmStateResult> {

    private final SiteNodeAlarmStateChanger siteNodeAlarmStateChanger;

    private final ViewNodeAlarmStateChanger viewNodeAlarmStateChanger;

    private final PhyLinkAlarmStateChanger phyLinkAlarmStateChanger;

    private final SiteLinkAlarmStateChanger siteLinkAlarmStateChanger;

    private final RackAlarmStateChanger rackAlarmStateChanger;

    private final OchLinkAlarmStateChanger ochLinkAlarmStateChanger;

    private final TunnelAlarmStateChanger tunnelAlarmStateChanger;

    @Autowired
    @Qualifier("stateUpdateExecutor")
    private ExecutorService executor;


    @Override
    public void changeState(AlarmStateResult state) {
        log.debug("start to change alarm state ,the state is :{}", state);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        RackAlarmState rackAlarmState = state.getRackAlarmState();
        if (rackAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> rackAlarmStateChanger.changeState(rackAlarmState), executor));
        }
        List<RackAlarmState> rackAlarmStates = state.getRackAlarmStateList();
        if (!CollectionUtils.isEmpty(rackAlarmStates)) {
            futures.add(CompletableFuture.runAsync(
                    () -> rackAlarmStateChanger.changeBatchState(rackAlarmStates), executor));
        }
        ViewNodeAlarmState viewNodeAlarmState = state.getViewNodeAlarmState();
        if (viewNodeAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> viewNodeAlarmStateChanger.changeState(viewNodeAlarmState), executor));
        }

        List<ViewNodeAlarmState> viewNodeAlarmStates = state.getViewNodeAlarmStates();
        if (!CollectionUtils.isEmpty(viewNodeAlarmStates)) {
            futures.add(CompletableFuture.runAsync(
                    () -> viewNodeAlarmStateChanger.changeBatchState(viewNodeAlarmStates),
                    executor));
        }

        SiteNodeAlarmState siteNodeAlarmState = state.getSiteNodeAlarmState();
        if (siteNodeAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> siteNodeAlarmStateChanger.changeState(siteNodeAlarmState), executor));
        }
        PhyLinksAlarmState phyLinksAlarmState = state.getPhyLinkAlarmState();
        if (phyLinksAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> phyLinkAlarmStateChanger.changeState(phyLinksAlarmState), executor));
        }
        SiteLinksAlarmState siteLinksAlarmState = state.getSiteLinkAlarmState();
        if (siteLinksAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> siteLinkAlarmStateChanger.changeState(siteLinksAlarmState), executor));
        }
        OchLinksAlarmState ochLinksAlarmState = state.getOchLinkAlarmState();
        if (ochLinksAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> ochLinkAlarmStateChanger.changeState(ochLinksAlarmState), executor));
        }
        TunnelsAlarmState tunnelsAlarmState = state.getTunnelAlarmState();
        if (tunnelsAlarmState != null) {
            futures.add(CompletableFuture.runAsync(
                    () -> tunnelAlarmStateChanger.changeState(tunnelsAlarmState), executor));
        }

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            log.error("Alarm state update failed", e);
                        } else {
                            log.debug("Alarm state update completed asynchronously");
                        }
                    });
        }
    }


}
