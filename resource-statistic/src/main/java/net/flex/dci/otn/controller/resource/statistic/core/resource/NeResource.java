package net.flex.dci.otn.controller.resource.statistic.core.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class NeResource extends AbstractResource {


    protected NeResource(SiteLinkDao siteLinkDao, TunnelDao tunnelDao, PhyNodeDao phyNodeDao,
            EquipmentsDao equipmentsDao,
            SubNetTreeNodeDao subNetTreeNodeDao) {
        super(siteLinkDao, tunnelDao, phyNodeDao, equipmentsDao, subNetTreeNodeDao);
    }

    public List<String> extractRelativeNeIds(String siteId, List<String> planeId) {
        log.debug("resolveNeFromSite({}) and subnet({})", siteId, planeId);
        if (siteId == null) {
            return new ArrayList<>();
        }
        List<Node> refPhyNodes = phyNodeDao.listOpPhyNodeBySiteNodeId(siteId);
        List<String> refNeIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(planeId)) {
            List<String> subNetDescendants = getSubNetDescendantIds(planeId);
            refNeIds = refPhyNodes.stream().filter(phyNode -> subNetDescendants.contains(
                            phyNode.getAugmentation(Node1.class).getPhysical().getPlaneId()))
                    .map(NodeAttributes::getNodeId)
                    .map(Uri::getValue)
                    .collect(
                            Collectors.toList());
        } else {
            refNeIds = refPhyNodes.stream().map(NodeAttributes::getNodeId)
                    .map(Uri::getValue).collect(
                            Collectors.toList());
        }
        return refNeIds;

    }


}
