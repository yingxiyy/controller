package net.flex.dci.otn.controller.implement.nbi.impl;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.List;

public class RepaireOnNeFriendlyName {
    private PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);

    public String start() {
        List<Node> cfgNodeList = phyNodeDao.listConfigPhyNodes();
        for (Node node : cfgNodeList) {
            NodeId nodeId = node.getNodeId();
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            if (!StringUtils.isEmpty(nodeAttr.getIp()) && !nodeAttr.getSupervisionStatus().equals(SupervisionStatusType.Unmonitored)) {
                if (phyNodeDao.existsOpNode(nodeId.getValue())) {
                    updateFriendlyName(node.getNodeId(), nodeAttr.getFriendlyName());
                }
            }
        }

        return "done ";
    }

    private void updateFriendlyName(NodeId nodeId, String friendlyName) {
        Node cfgNode = new NodeBuilder()
                .setNodeId(nodeId)
                .setKey(new NodeKey(nodeId))
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setFriendlyName(friendlyName)
                                .setProperties(PropertyTool.addProperty(null, "hostName", friendlyName))
                                .build())
                        .build())
                .build();
        neMgr.configNe(cfgNode);
    }
}
