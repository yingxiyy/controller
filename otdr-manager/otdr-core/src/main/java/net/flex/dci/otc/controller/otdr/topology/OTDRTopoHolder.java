package net.flex.dci.otc.controller.otdr.topology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * 2026/9/1
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OTDRTopoHolder implements Serializable {

    private Map<String, Node> phyNodeById;

    private Map<String, Node> siteByNodeId;

    private Map<String, Map<String, TerminationPoint>> nodeTpsCache;

    private Set<String> OTDRTps;


    private Map<String, OtsLinkTerminationPointInfo> otsLinkTpInfoById;


    public Node getPhyNodeById(String phyNodeId) {
        return phyNodeById.get(phyNodeId);
    }

    public Node getSiteById(String siteId) {
        return siteByNodeId.get(siteId);
    }

    public Map<String, TerminationPoint> getNodeTps(String neId) {
        return nodeTpsCache.computeIfAbsent(neId, k -> new HashMap<>());
    }

    public Set<String> getOtdrTps() {
        return OTDRTps;
    }

    public OtsLinkTerminationPointInfo getOtsLinkTpInfo(String linkId) {
        return otsLinkTpInfoById == null ? null : otsLinkTpInfoById.get(linkId);
    }

}
