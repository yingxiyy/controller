package net.flex.dci.otn.controller.nms.enrich.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.enrich.TopologyOperationEnrichHandler;
import net.flex.dci.otn.controller.nms.enrich.components.LinkPropertiesEnrich;
import net.flex.dci.otn.controller.nms.enrich.components.NodePropertiesEnrich;
import net.flex.dci.otn.controller.nms.enrich.components.TunnelPropertiesEnrich;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

/**
 * enrich the properties for the data properties
 *
 * @version 1.0
 * @date 2022/12/20 10:29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TopologyOperationEnrichHandlerImpl implements TopologyOperationEnrichHandler {

    private final TunnelPropertiesEnrich tunnelPropertiesEnrich;

    private final LinkPropertiesEnrich linkPropertiesEnrich;

    private final NodePropertiesEnrich nodePropertiesEnrich;

    @Override
    public DataObject enrichProperties(DataObject dataObject) {
        log.debug("start to enrich properties ");
        DataObject enrichedDataObject = null;
        if (dataObject instanceof Tunnel) {
            //enrich the tunnel
            enrichedDataObject = tunnelPropertiesEnrich.enrichProperties(dataObject);
        } else if (dataObject instanceof Link) {
            enrichedDataObject = linkPropertiesEnrich.enrichProperties(dataObject);
        } else if (dataObject instanceof Node) {
            enrichedDataObject = nodePropertiesEnrich.enrichProperties(dataObject);
        } else {
            enrichedDataObject = dataObject;
        }
        return enrichedDataObject;
    }
}
