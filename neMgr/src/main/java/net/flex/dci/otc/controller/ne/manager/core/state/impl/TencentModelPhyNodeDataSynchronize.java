package net.flex.dci.otc.controller.ne.manager.core.state.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.ne.manager.components.PhyNodeManager;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.core.state.AbstractPhyNodeDataSynchronize;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2023 4:19 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TencentModelPhyNodeDataSynchronize extends AbstractPhyNodeDataSynchronize {

    private final PhyNodeManager phyNodeManager;

    private final PhyNodeDao phyNodeDao;

    private final CommunicateStateUpdater communicateStateUpdater;

    @Override
    public NeYangModel supportNeYangModel() {
        return NeYangModel.Tencent;
    }

    @Override
    public void synchronizingData(Node phyNe) {
        String neId = phyNe.getNodeId().getValue();
        log.debug("start to synchronizing the tencent model data ,The ne id is:{}", neId);
        if (phyNe.getTerminationPoint() == null) {
            //todo merge from op to config
            Node operationalNode = phyNodeManager.mergeConfNeDataFromOp(phyNe, neId);
            phyNodeDao.rewriteConfigPhyNode(operationalNode);
            updateNodeStatusImplement(phyNe);
        } else {
            updateNodeStatus(phyNe);
        }
//        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.SyncFinished);
        communicateStateUpdater.synced(neId);
    }


    private void updateNodeStatusImplement(Node phyNe) {
        String neId = phyNe.getNodeId().getValue();
        log.debug("update config ne node status ,the neId is :{}", neId);
        Node realNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical emlNePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), emlNePhysical.getImplementState(),
                AdminStatus.Up, emlNePhysical.getAlignmentStatus());
    }
}
