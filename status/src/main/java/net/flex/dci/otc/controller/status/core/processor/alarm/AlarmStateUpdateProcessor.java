package net.flex.dci.otc.controller.status.core.processor.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.calculator.alarm.AlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.changer.alarm.AlarmStateChanger;
import net.flex.dci.otc.controller.status.core.handler.StateChangeChainHandler;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/4 13:58
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmStateUpdateProcessor {

    private final AlarmStateCalculator alarmStateCalculator;

    private final AlarmStateChanger alarmStateChanger;

    private final StateChangeChainHandler dciStateChainHandler;

    private final PhyNodeDao phyNodeDao;

    public void process(String phyNodeId) {
        Node phyNode = phyNodeDao.getConfigPhyNodeById(phyNodeId);
//        AlarmStateCalculatorResult result = alarmStateCalculator.calculate(
//                phyNode);
//        alarmStateChanger.changeState(result);
        dciStateChainHandler.executeAllStateChangeChainHandle(phyNode);
    }

}
