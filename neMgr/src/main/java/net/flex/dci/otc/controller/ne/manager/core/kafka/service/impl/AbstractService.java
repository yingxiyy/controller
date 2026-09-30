package net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 2022/3/30 10:27
 */

public abstract class AbstractService {


    public abstract void sendNotification(Node phyNode);
}
