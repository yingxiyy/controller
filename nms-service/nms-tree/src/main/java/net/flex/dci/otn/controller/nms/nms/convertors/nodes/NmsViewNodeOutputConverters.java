package net.flex.dci.otn.controller.nms.nms.convertors.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dto.NeSubTypeInfo;
import net.flex.dci.otc.mongo.dto.NeSubTypeQuery;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.SiteRoleBitCalcUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2025/8/15
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class NmsViewNodeOutputConverters extends
        AbstractNmsOutputConverters<Node, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> {

    @Autowired
    private SiteNodeDao siteNodeDao;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.VIEW_SITE;
    }

    @Override
    public List<Node> convert2NmsOutput(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodeList) {
        log.debug("convert to view node output");
        List<Node> viewNodes = new ArrayList<>();
        if (nodeList == null || nodeList.isEmpty()) {
            return viewNodes;
        }

        Map<String, String> nodeId2RealSiteId = new HashMap<>(nodeList.size());
        Map<String, String> nodeId2SubnetId = new HashMap<>(nodeList.size());
        Map<String, View> nodeId2View = new HashMap<>(nodeList.size());

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : nodeList) {
            if (node == null || node.getNodeId() == null) {
                continue;
            }

            String nodeId = node.getNodeId().getValue();
            Node1 node1 = node.getAugmentation(Node1.class);
            View view = node1 == null ? null : node1.getView();
            String subnetId = view == null ? null : view.getSubnetId();
            String realSiteId = ViewNodeNamingRule.extractSiteId(nodeId);

            nodeId2RealSiteId.put(nodeId, realSiteId);
            nodeId2SubnetId.put(nodeId, subnetId);
            nodeId2View.put(nodeId, view);
        }

        List<NeSubTypeQuery> queryList = new ArrayList<>();
        for (Map.Entry<String, String> entry : nodeId2SubnetId.entrySet()) {
            String subnetId = entry.getValue();
            if (StringUtils.hasText(subnetId)) {
                String siteId = nodeId2RealSiteId.get(entry.getKey());
                queryList.add(new NeSubTypeQuery(siteId, subnetId));
            }
        }
        final Map<String, List<NeSubTypeInfo>> neSubTypeMap;
        if (!queryList.isEmpty()) {
            neSubTypeMap = phyNodeDao.batchListNeSubTypeBySiteAndSubnet(queryList);
        } else {
            neSubTypeMap = new HashMap<>();
        }
//        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> siteNodes = siteNodeDao.listSiteNodes();
//        Map<String, SiteNodeAttribute> nodeSiteTypeMap = getNodeSiteTypeMap(siteNodes);
        nodeList.forEach(node -> {
            String nodeId = node.getNodeId().getValue();
            String realSiteId = nodeId2RealSiteId.get(nodeId);
            String subnetId = nodeId2SubnetId.get(nodeId);
            NodeBuilder nodeBuilder = new NodeBuilder(node);
            View view = nodeId2View.get(nodeId);
            SiteType siteType = SiteType.SITE;
            boolean hasNe = false;
            if (StringUtils.hasText(subnetId)) {
//                List<NeSubTypeInfo> neSubTypeInfos = phyNodeDao.listAllNeSubTypeBySiteAndSubnet(
//                        realNodeId, subnetId);
//                siteType = SiteRoleBitCalcUtil.calcByNeSubType(
//                        neSubTypeInfos.stream().map(NeSubTypeInfo::getNeSubType)
//                                .collect(
//                                        Collectors.toList()));
//                nodeBuilder.setHasNetworkElements(!neSubTypeInfos.isEmpty());
                String key = buildQueryKey(realSiteId, subnetId);
                List<NeSubTypeInfo> neSubTypeInfos = neSubTypeMap.getOrDefault(key,
                        Collections.emptyList());

                siteType = SiteRoleBitCalcUtil.calcByNeSubType(
                        neSubTypeInfos.stream().map(NeSubTypeInfo::getNeSubType)
                                .collect(Collectors.toList())
                );
                hasNe = !neSubTypeInfos.isEmpty();
            }
            nodeBuilder.setSiteType(siteType);
            nodeBuilder.setView(view);
            nodeBuilder.setHasNetworkElements(hasNe);
            viewNodes.add(nodeBuilder.build());
        });
        return viewNodes;
    }

    private String buildQueryKey(String siteId, String subnetId) {
        return siteId + "|" + subnetId;
    }

    private Map<String, SiteNodeAttribute> getNodeSiteTypeMap(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> siteNodes) {
        Map<String, SiteNodeAttribute> nodeSiteTypeMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node site : siteNodes) {
            String nodeId = site.getNodeId().getValue();
            Site sitePhysical = site.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                    .getSite();
            SiteType siteType = sitePhysical.getSiteType();
            boolean hasNe = !site.getSupportingNode().isEmpty();
            nodeSiteTypeMap.put(nodeId,
                    SiteNodeAttribute.builder().siteType(siteType).hasNes(hasNe).build());
        }
        return nodeSiteTypeMap;
    }


    @Data
    @Builder
    private static class SiteNodeAttribute {

        private SiteType siteType;

        private boolean hasNes;
    }
}
