package net.flex.dci.otc.controller.ne.manager.core.state;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 11/17/2023 4:19 PM
 */
@Slf4j
public abstract class AbstractPhyNodeDataSynchronize implements PhyNodeDataSynchronize {


    @Autowired
    protected PhyNodeDao phyNodeDao;

    public abstract NeYangModel supportNeYangModel();


    protected void updateNodeStatus(Node phyNe) {
        String neId = phyNe.getNodeId().getValue();
        log.debug("update config ne node status ,the neId is :{}", neId);
        Node realNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical emlNePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        ImplementState implementState = phyNe.getAugmentation(Node1.class).getPhysical()
                .getImplementState();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), implementState,
                AdminStatus.Up, emlNePhysical.getAlignmentStatus());
    }

}
