package net.flex.dci.otc.controller.ne.manager.core.state;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 2022/5/31 10:03
 */
public interface AlignStateSynchronize {

    void syncAlignState(Node dbNode);
}
