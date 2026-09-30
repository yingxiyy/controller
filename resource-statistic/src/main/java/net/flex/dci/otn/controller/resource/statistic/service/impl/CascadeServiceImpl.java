package net.flex.dci.otn.controller.resource.statistic.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.resource.statistic.dto.NodeInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteLinkInfo;
import net.flex.dci.otn.controller.resource.statistic.service.CascadeService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/3/2025 1:46 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CascadeServiceImpl implements CascadeService {

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;

    private final SiteNodeDao siteNodeDao;

    private final PhyNodeDao phyNodeDao;

    private final SubNetTreeNodeDao subNetTreeNodeDao;


    @Override
    public List<SiteInfo> retrieveSiteByNetwork(String network) {
        log.debug("retrieve site by network:{}", network);
        List<net.flex.dci.otc.mongo.dto.SiteInfo> siteInfoDbs = new ArrayList<>();

        List<String> refPhyNodeIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(
                Collections.singletonList(network));
        List<String> siteId = refPhyNodeIds.stream().map(PhysicalNodeIdNamingRule::getSiteId)
                .distinct()
                .collect(
                        Collectors.toList());
        siteInfoDbs = siteNodeDao.getSiteFriendlyNameByIds(
                siteId);
        List<SiteInfo> siteInfos = siteInfoDbs.stream()
                .map(siteInfo -> SiteInfo.builder().siteName(siteInfo.getSiteName())
                        .id(siteInfo.getSiteId()).build())
                .sorted(Comparator.comparing(SiteInfo::getSiteName))
                .collect(Collectors.toList());
        return siteInfos;
    }

    @Override
    public List<NodeInfo> getSiteSubPhyNodeBySite(String siteId, String subnetId) {
        log.debug("retrieve sub phy node by site:{}", siteId);
        List<String> refPhyNodeIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(
                Collections.singletonList(subnetId));
        List<String> siteRefPhyNodeIds = refPhyNodeIds.stream()
                .filter(neId -> neId.contains(siteId)).collect(
                        Collectors.toList());
        List<Node> configNode = phyNodeDao.listLightConfigPhyNodeByIds(siteRefPhyNodeIds);
        List<NodeInfo> nodeInfos = configNode.stream().map(phyNode -> {
                    Physical physical = phyNode.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                            .getPhysical();
                    return NodeInfo.builder().neId(phyNode.getNodeId().getValue()).neName(
                            physical.getFriendlyName()).build();
                })
                .sorted(Comparator.comparing(NodeInfo::getNeName))
                .collect(Collectors.toList());
        return nodeInfos;
    }

    @Override
    public List<SiteLinkInfo> getRefSiteLinkInfoBySubnet(String subnetId) {
        log.debug("retrieve site link info by subnet:{}", subnetId);
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdBySubnetId(subnetId);
        List<LinkStateDto> linkStateDtos = siteLinkDao.getSiteLinkStateByIds(siteLinkIds);
        List<SiteLinkInfo> siteLinkInfos = linkStateDtos.stream()
                .map(linkStateDto -> SiteLinkInfo.builder().siteLinkId(
                        linkStateDto.getId()).siteLinkName(linkStateDto.getFriendlyName()).build())
                .sorted(Comparator.comparing(SiteLinkInfo::getSiteLinkName))
                .collect(
                        Collectors.toList());
        return siteLinkInfos;
    }


}
