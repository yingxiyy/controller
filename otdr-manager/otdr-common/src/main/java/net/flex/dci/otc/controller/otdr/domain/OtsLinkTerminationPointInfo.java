package net.flex.dci.otc.controller.otdr.domain;

import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * 2026/9/24
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkTerminationPointInfo implements Serializable {

    private String edfaSourceTp;

    private String edfaDestTp;

    private String ramanSourceTp;

    private String ramanDestTp;

    private Map<String, Node> nodeMap;
}
