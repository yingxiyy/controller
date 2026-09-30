package net.flex.dci.otc.controller.status.core.handler;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otc.controller.status.core.changer.StateChanger;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/6 16:27
 */
@Component
@Setter
@Getter
@Slf4j
public abstract class AbstractStateChangeChainHandler {

    public static final int LOWEST_PRECEDENCE = -2147483648;

    public static final int HIGHEST_PRECEDENCE = 2147483646;
    @Autowired
    protected StateChanger stateChanger;
    private AbstractStateChangeChainHandler next;

    public abstract int getOrder();

    public abstract void stateChange(Node phyNode);

    public void alarmStateChange(String nmlKey, List<Alarm> alarmList, PhyNodeCache phyNodeCache) {
        log.debug("default relative  alarm change state");
    }

    public void alarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
        log.debug("default relative link alarm change state");
    }

    public void alarmStateChange(List<String> linkIds) {
        log.debug("default relative link alarm change state");
    }

    public void electricalLayerAlarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
        log.debug("electrical layer alarm state change");
    }

    public void alarmClearStateChange(String nmlKey, List<Alarm> alarmList,
            PhyNodeCache phyNodeCache) {
        log.debug("default relative alarm clear change state");
    }

    public void electricalLayerAlarmClearStateChange(List<String> linkIds) {
        log.debug("electrical layer alarm state change");
    }

    public void alarmClearStateChange(List<String> linkIds) {
        log.debug("default relative link alarm clear change state");
    }

    public void phyNodeRemoveStateChange(String neId) {

    }

    public void statusEventChange(StatusChangeEvent statusChangeEvent) {
        log.debug("default relative statusEvent change ");
    }

    public void statusEventChange(List<String> linkIds, OperStatus operStatus) {

    }

    public void statusEventChange(List<String> linkId) {
        log.debug("default relative statusEvent link id operation status change ");
    }

}
