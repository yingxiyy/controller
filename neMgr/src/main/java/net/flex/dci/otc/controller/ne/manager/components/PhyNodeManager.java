package net.flex.dci.otc.controller.ne.manager.components;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeInput;

/**
 * @version 1.0
 * @date 7/26/2023 10:27 AM
 */
public interface PhyNodeManager {

    Node mergeConfNeDataFromOp(Node configNode, String neId);


    Node createNewNode(CreateNeInput input);

    Node mountNeToSite(String neId);
}
