package net.flex.dci.otc.controller.otdr.validator;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 11/2/2023 2:14 PM
 */
public interface OTDRValidator {

    void validateMonitorSituation(String monitorPortId, Node node);
}
