/*
 * Copyright (c) 2019 Network Flex Any Comp. and others.
 */
package net.flex.dci.otn.controller.implement.common.utils;

import java.util.ArrayList;
import java.util.List;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.PhysicalNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

/** Applies the retained APS member state required after a protection-leg change. */
public final class ApsProtectionState {

    private ApsProtectionState() {
    }

    /** Restores the protection1to2 APS member switches after 3->2. */
    public static CrossConnections restoreTwoLegMemberProperties(CrossConnections xc) {
        if (xc == null || xc.getAps() == null) {
            return xc;
        }

        Properties properties = restoreTwoLegMemberProperties((CrossConnectionAttributes) xc);
        return new CrossConnectionsBuilder(xc)
                .setAps(new ApsBuilder(xc.getAps()).setProperties(properties).build())
                .build();
    }

    /** Calculates the shared APS member properties for physical and embedded route XCs. */
    public static Properties restoreTwoLegMemberProperties(CrossConnectionAttributes xc) {
        if (xc == null || xc.getAps() == null) {
            return null;
        }

        Properties properties = xc.getAps().getProperties();
        List<TpId> memberTps = new ArrayList<>();
        if (xc.getSourceTp() != null) {
            xc.getSourceTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }
        if (xc.getDestinationTp() != null) {
            xc.getDestinationTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }
        for (TpId tpId : memberTps) {
            String value = tpId.getValue();
            String member = value.substring(value.lastIndexOf('-') + 1);
            if (member.endsWith("A") || member.endsWith("B")) {
                properties = PropertyTool.addProperty(properties, member + ".enabled", "true");
            } else if (member.endsWith("C")) {
                properties = PropertyTool.addProperty(properties, member + ".enabled", "false");
            }
        }
        return properties;
    }

    /** Persists the two-leg LOP state on the shared APS XC and its C member TP. */
    public static Node restoreTwoLegState(Node node, String apsXcId, String cTpId) {
        Node updatedNode = new PhysicalNode(ImplActionType.Implement).updateTpImplState(
                node, cTpId, AdminStatus.Down, ImplementState.Allocate);
        Physical physical = updatedNode.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> xcs = new ArrayList<>();
        for (CrossConnections xc : physical.getCrossConnections()) {
            if (xc.getCrossConnectionId().getValue().equals(apsXcId)) {
                xcs.add(new CrossConnectionsBuilder(restoreTwoLegMemberProperties(xc))
                        .setAdminState(AdminStatus.Up)
                        .setImplementState(ImplementState.Implement)
                        .build());
            } else {
                xcs.add(xc);
            }
        }
        return new NodeBuilder(updatedNode).addAugmentation(Node1.class,
                new Node1Builder(updatedNode.getAugmentation(Node1.class))
                        .setPhysical(new PhysicalBuilder(physical)
                        .setCrossConnections(xcs).build()).build()).build();
    }

    /** Updates only APS member properties and preserves an unimplemented XC's top-level state. */
    public static Node restoreTwoLegMemberProperties(Node node, String apsXcId) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> xcs = new ArrayList<>();
        for (CrossConnections xc : physical.getCrossConnections()) {
            xcs.add(xc.getCrossConnectionId().getValue().equals(apsXcId)
                    ? restoreTwoLegMemberProperties(xc) : xc);
        }
        return new NodeBuilder(node).addAugmentation(Node1.class,
                new Node1Builder(node.getAugmentation(Node1.class))
                        .setPhysical(new PhysicalBuilder(physical)
                                .setCrossConnections(xcs).build()).build()).build();
    }
}
