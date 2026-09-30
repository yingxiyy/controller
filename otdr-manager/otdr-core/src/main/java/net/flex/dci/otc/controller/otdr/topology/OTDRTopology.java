package net.flex.dci.otc.controller.otdr.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.ScanPortHelper;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.springframework.stereotype.Component;

/**
 * 2026/9/1
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRTopology {

    private final SiteNodeDao siteNodeDao;

    private final PhyNodeDao phyNodeDao;

    private final TerminationPointDao terminationPointDao;

    private final ScanPortHelper scanPortHelper;

    public OTDRTopoHolder preload(List<Link> otsLinks) {
        log.info("preload the ots link relative things site ne tp link :{}", otsLinks.size());
        List<String> linkIds = otsLinks.stream().map(link -> link.getLinkId().getValue()).collect(
                Collectors.toList());
        Set<String> siteIds = new HashSet<>();
        Set<String> phyNodeIds = new HashSet<>();
        Set<String> terminationPointIds = new HashSet<>();
        Map<String, OtsLinkTerminationPointInfo> otsLinkTpInfoById = new HashMap<>();
        for (String linkId : linkIds) {
            String sourceSite = PhysicalLinkIdNamingRule.getSiteAId(linkId);
            String destSite = PhysicalLinkIdNamingRule.getSiteZId(linkId);
            String sourceNe = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            String destNe = PhysicalLinkIdNamingRule.getNodeZId(linkId);
            String srcTp = PhysicalLinkIdNamingRule.getTpAId(linkId);
            String destTp = PhysicalLinkIdNamingRule.getTpZId(linkId);
            siteIds.add(sourceSite);
            siteIds.add(destSite);
            phyNodeIds.add(sourceNe);
            phyNodeIds.add(destNe);
            terminationPointIds.add(srcTp);
            terminationPointIds.add(destTp);
            OtsLinkTerminationPointInfo tpInfo = scanPortHelper.resolveOtsLinkEndpoint(linkId);
            terminationPointIds.add(tpInfo.getEdfaSourceTp());
            terminationPointIds.add(tpInfo.getEdfaDestTp());
            otsLinkTpInfoById.put(linkId, tpInfo);
        }
        //load cache for the site node
        List<Node> siteNode = siteNodeDao.listAllLightNodeByIds(new ArrayList<>(siteIds));
        Map<String, Node> siteMap = siteNode.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));

        List<Node> phyNode = phyNodeDao.listLightOpPhyNodeByIds(new ArrayList<>(phyNodeIds));
        Map<String, Node> phyNodeMap = phyNode.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));

        Map<String, Map<String, TerminationPoint>> tpMap = terminationPointDao.batchGetOpNeTpMap(
                phyNodeIds, terminationPointIds);

        return OTDRTopoHolder.builder().phyNodeById(phyNodeMap).siteByNodeId(siteMap)
                .OTDRTps(terminationPointIds)
                .otsLinkTpInfoById(otsLinkTpInfoById)
                .nodeTpsCache(tpMap).build();
    }

}
