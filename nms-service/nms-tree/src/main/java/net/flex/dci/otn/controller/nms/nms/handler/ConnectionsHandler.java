/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOpsConnectionsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ops.connections.output.OpsConnections;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ops.connections.output.OpsConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ops.connections.output.OpsConnectionsKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ops.connection.MyPortBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ops.connection.PeerPortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.idc.SubInterface;
import org.springframework.stereotype.Component;


/**
 * @author: xinyzhao
 * @date: 2021/4/9
 */
@Slf4j
@Component
public class ConnectionsHandler extends AbstractBaseHandler {

    private String nodeId;

    public ConnectionsHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<OpsConnections> getOpsConnections(GetOpsConnectionsInput input) throws Exception {
        Node node = netconfTopology.getPhyNode(input.getPhyNodeId());
        if (node == null) {
            throw new Exception(
                    "cannot find out the required NE " + input.getPhyNodeId());
        }

        Node1 phyNode = node.getAugmentation(Node1.class);
        if (phyNode == null && phyNode.getPhysical() == null) {
            throw new Exception(
                    "cannot find out the required PHY NE " + input.getPhyNodeId());
        }

//        if (!phyNode.getPhysical().getNodeType().equals(NodeType.OPS) && !phyNode.getPhysical()
//                .getNodeType().equals(NodeType.OPSSWITCH)) {
//            throw new Exception(
//                    "The NE is not OPS NE. " + phyNode.getPhysical().getFriendlyName());
//        }

        nodeId = input.getPhyNodeId();
        List<OpsConnections> links = new LinkedList<>();
        if (phyNode.getPhysical().getInternalLinks() != null) {
            for (InternalLinks il : phyNode.getPhysical().getInternalLinks()) {
                links.add(constructLink(il));
            }
        }
        return links;
    }

    /**
     * construct link
     *
     * @param il
     * @return
     */
    private OpsConnections constructLink(InternalLinks il) throws Exception {
        OpsConnectionsBuilder lb = new OpsConnectionsBuilder();
        if (il.getSrcTp().contains(nodeId)) {
            constructMyPort(lb, il.getSrcTp(), il.getProperties(), true);
            constructPeerPort(lb, il.getDstTp(), il.getProperties(), false);
        } else {
            constructPeerPort(lb, il.getSrcTp(), il.getProperties(), true);
            constructMyPort(lb, il.getDstTp(), il.getProperties(), false);
        }
        lb.setLinkRef(il.getLinkRef());
        lb.setKey(new OpsConnectionsKey(lb.getLinkRef()));
        return lb.build();
    }

    private void constructMyPort(
            OpsConnectionsBuilder lb,
            String tpId, Properties properties, boolean isSrc) throws Exception {
        String[] ids = tpId.split("#");
        String nodeId = ids[0] + "#" + ids[1];
        Node node = netconfTopology.getPhyNode(nodeId);
        if (node == null) {
            throw new Exception(
                    "cannot find out the required NE " + nodeId);
        }

        Node1 phyNode = node.getAugmentation(Node1.class);
        if (phyNode == null && phyNode.getPhysical() == null) {
            throw new Exception(
                    "cannot find out the required PHY NE " + nodeId);
        }

        TerminationPoint tp = netconfTopology
                .getTerminationPoint(TopoNameConstants.Phy_Topo_Key, nodeId, tpId);
        if (tp == null) {
            throw new Exception(
                    "cannot find out the required TP " + tpId);
        }
        TerminationPoint1 phyTp = tp.getAugmentation(TerminationPoint1.class);
        if (phyTp == null && phyTp.getPhysical() == null) {
            throw new Exception(
                    "cannot find out the required PHY TP " + tpId);
        }

        MyPortBuilder mb = new MyPortBuilder();
        mb.setNodeId(nodeId);
        mb.setNodeFriendlyName(phyNode.getPhysical().getFriendlyName());
        mb.setInterfaceRef(
                phyTp.getPhysical().getDcn().getInterfaceRef() != null ? phyTp.getPhysical()
                        .getDcn().getInterfaceRef() : phyTp.getPhysical().getFriendlyName());

        String tunnelName = null;
        if (properties != null && properties.getProperty() != null) {
            if (isSrc) {
                for (Property p : properties.getProperty()) {
                    if (p.getName().toUpperCase().equals("SRC_TUNNEL")) {
                        tunnelName = p.getValue();
                    }
                }
            } else {
                for (Property p : properties.getProperty()) {
                    if (p.getName().toUpperCase().equals("DEST_TUNNEL")) {
                        tunnelName = p.getValue();
                    }
                }
            }
        }
        //normally the port 53/54 should include tunnel
        if (tunnelName != null) {
            if (phyTp.getPhysical().getIdc().getSubInterface() == null) {
                throw new Exception(
                        "the TP should include tunnel sub-interface " + tpId);
            }

            for (SubInterface si : phyTp.getPhysical().getIdc().getSubInterface()) {
                if (si.getInterfaceRef().equals(tunnelName)) {
                    mb.setInterfaceRef(mb.getInterfaceRef() + "#" + tunnelName);
                    mb.setIp(si.getIp());
                    mb.setPrefixLength(si.getPrefixLength());
                    break;
                }
            }
        } else {
            mb.setIp(phyTp.getPhysical().getDcn().getIp());
            mb.setPrefixLength(phyTp.getPhysical().getDcn().getPrefixLength());
        }
        lb.setMyPort(mb.build());
    }

    private void constructPeerPort(
            OpsConnectionsBuilder lb,
            String tpId, Properties properties, boolean isSrc) throws Exception {
        String[] ids = tpId.split("#");
        String nodeId = ids[0] + "#" + ids[1];
        Node node = netconfTopology.getPhyNode(nodeId);
        if (node == null) {
            throw new Exception(
                    "cannot find out the required NE " + nodeId);
        }

        Node1 phyNode = node.getAugmentation(Node1.class);
        if (phyNode == null && phyNode.getPhysical() == null) {
            throw new Exception(
                    "cannot find out the required PHY NE " + nodeId);
        }

        TerminationPoint tp = netconfTopology
                .getTerminationPoint(TopoNameConstants.Phy_Topo_Key, nodeId, tpId);
        if (tp == null) {
            throw new Exception(
                    "cannot find out the required TP " + tpId);
        }
        TerminationPoint1 phyTp = tp.getAugmentation(TerminationPoint1.class);
        if (phyTp == null && phyTp.getPhysical() == null) {
            throw new Exception(
                    "cannot find out the required PHY TP " + tpId);
        }

        PeerPortBuilder pb = new PeerPortBuilder();
        pb.setNodeId(nodeId);
        pb.setNodeFriendlyName(phyNode.getPhysical().getFriendlyName());
        pb.setInterfaceRef(
                phyTp.getPhysical().getDcn().getInterfaceRef() != null ? phyTp.getPhysical()
                        .getDcn().getInterfaceRef() : phyTp.getPhysical().getFriendlyName());

        String tunnelName = null;
        if (properties != null && properties.getProperty() != null) {
            if (isSrc) {
                for (Property p : properties.getProperty()) {
                    if (p.getName().toUpperCase().equals("SRC_TUNNEL")) {
                        tunnelName = p.getValue();
                    }
                }
            } else {
                for (Property p : properties.getProperty()) {
                    if (p.getName().toUpperCase().equals("DEST_TUNNEL")) {
                        tunnelName = p.getValue();
                    }
                }
            }
        }
        //normally the port 53/54 should include tunnel
        if (tunnelName != null) {
            if (phyTp.getPhysical().getIdc().getSubInterface() == null) {
                throw new Exception(
                        "the TP should include tunnel sub-interface " + tpId);
            }

            for (SubInterface si : phyTp.getPhysical().getIdc().getSubInterface()) {
                if (si.getInterfaceRef().equals(tunnelName)) {
                    pb.setInterfaceRef(pb.getInterfaceRef() + "#" + tunnelName);
                    pb.setIp(si.getIp());
                    pb.setPrefixLength(si.getPrefixLength());
                    break;
                }
            }
        } else {
            pb.setIp(phyTp.getPhysical().getDcn().getIp());
            pb.setPrefixLength(phyTp.getPhysical().getDcn().getPrefixLength());
        }
        lb.setPeerPort(pb.build());
    }
}
