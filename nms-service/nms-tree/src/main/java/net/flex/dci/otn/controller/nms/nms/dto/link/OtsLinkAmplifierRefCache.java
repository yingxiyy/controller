package net.flex.dci.otn.controller.nms.nms.dto.link;

import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * 2026/6/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkAmplifierRefCache implements Serializable {

    private Map<String, Node> phyNodeMap;

    private Map<String, Node> phyNodeSiteNodeMap;
}
