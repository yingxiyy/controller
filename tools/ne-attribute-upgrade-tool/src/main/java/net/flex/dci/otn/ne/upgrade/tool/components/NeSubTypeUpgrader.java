package net.flex.dci.otn.ne.upgrade.tool.components;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * 2026/3/14
 *
 * @author musa
 * @version 1.0
 **/
public interface NeSubTypeUpgrader {

    void upgradeNeSubType(List<Node> nodes);

    NodeType generalType();

    void upgradeNeSubType(Node node);
}
