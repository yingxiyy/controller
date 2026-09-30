package net.flex.dci.otn.controller.implement.common.utils;

import java.util.List;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NeManagementChecker {

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private ImplConfig implConfig;

    public void checking(List<String> nodeIdList) {
        for (String nodeId : nodeIdList) {
            Node cfgNode = phyNodeDao.getConfigPhyNodeById(nodeId);
            if (cfgNode == null) {
                String errMsg = String.format("We haven't found the device in database (%s)",
                        nodeId);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errMsg);
            }

            Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
            if (implConfig.isWriteWithoutIP()) {
                return;
            }

            if (cfgNodeAttr.getIp() == null) {
                String errMsg = String.format("The Device without IP %s (%s)",
                        cfgNodeAttr.getFriendlyName(),
                        cfgNode.getNodeId().getValue());
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errMsg);
            } else {
                if (!phyNodeDao.existsOpNode(nodeId)) {
                    String errMsg = String.format(
                            "We haven't build connection with the device, please check on %s (%s)",
                            cfgNodeAttr.getFriendlyName(),
                            cfgNode.getNodeId().getValue());
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errMsg);
                }
                Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
                Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();
                if (!CommunicationStatusType.SyncFinished.equals(
                        opNodeAttr.getCommunicationStatus())) {
                    // Existing OP node only proves the NE was seen; downloads require a fully synced NE.
                    String errMsg = String.format(
                            "The device has not finished sync, please check on %s (%s) status %s",
                            cfgNodeAttr.getFriendlyName(),
                            cfgNode.getNodeId().getValue(),
                            opNodeAttr.getCommunicationStatus());
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errMsg);
                }
            }
        }
    }
}
