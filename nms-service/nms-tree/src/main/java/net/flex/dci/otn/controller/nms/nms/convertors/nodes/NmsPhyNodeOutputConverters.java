package net.flex.dci.otn.controller.nms.nms.convertors.nodes;

import static net.flex.dci.otn.controller.nms.utils.Constants.ADAPTER_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.COLLECTOR_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.CURRENT_SOFTWARE_VERSION;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/3/23 10:33
 */
@Slf4j
@Component
public class NmsPhyNodeOutputConverters extends
        AbstractNmsOutputConverters<Node, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> {


    @Override
    public List<Node> convert2NmsOutput(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> elements) {
        log.info("start to phy node to nms output phy node");
        if (elements.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> neIds = elements.stream().map(NodeAttributes::getNodeId).map(Uri::getValue)
                .collect(
                        Collectors.toList());
        Map<String, PhyNodeCache> neCacheMap = dciTopologyCacheManager.batchGetValues(neIds,
                PhyNodeCache.class);
        List<Node> nmsPhyNodes = elements.stream().map(phyNode -> {
            NodeBuilder phyNodeBuilder = new NodeBuilder();
            phyNodeBuilder.fieldsFrom(phyNode);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                    phyNode.getAugmentation(
                            Node1.class).getPhysical());
            physicalBuilder.setInternalLinks(null);
            physicalBuilder.setCrossConnections(null);
            physicalBuilder.setEquipments(null);
            physicalBuilder.setOCMGripGroups(null);

            physicalBuilder.setProperties(getPhyNodeAdditionalProperties(phyNode, neCacheMap));
//            List<TerminationPoint> terminationPoints = getTerminationPoint(
//                    phyNode.getTerminationPoint());
            phyNodeBuilder.setTerminationPoint(null);
            phyNodeBuilder.setPhysical(physicalBuilder.build());
            phyNodeBuilder.setNodeId(phyNode.getNodeId());
            phyNodeBuilder.setSupportingNode(null);
            phyNodeBuilder.setTopologyRef(new TopologyId(PHY_TOPO_KEY));

            return phyNodeBuilder.build();
        }).collect(Collectors.toList());
        return nmsPhyNodes;

    }

    @Override
    public List<Node> convert2FullNmsOutput(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> elements) {
        log.info("start to phy node to nms output phy node");
        if (elements.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> neIds = elements.stream().map(NodeAttributes::getNodeId).map(Uri::getValue)
                .collect(
                        Collectors.toList());
        Map<String, PhyNodeCache> neCacheMap = dciTopologyCacheManager.batchGetValues(neIds,
                PhyNodeCache.class);
        List<Node> nmsPhyNodes = elements.stream().map(phyNode -> {
            NodeBuilder phyNodeBuilder = new NodeBuilder();
            phyNodeBuilder.fieldsFrom(phyNode);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                    phyNode.getAugmentation(
                            Node1.class).getPhysical());
            phyNodeBuilder.setSupportingNode(null);
            physicalBuilder.setInternalLinks(null);
            physicalBuilder.setCrossConnections(null);
//            physicalBuilder.setEquipments(null);
//            physicalBuilder.setOCMGripGroups(null);
            physicalBuilder.setProperties(getPhyNodeAdditionalProperties(phyNode, neCacheMap));
            List<TerminationPoint> terminationPoints = getTerminationPoint(
                    phyNode.getTerminationPoint());
            phyNodeBuilder.setTerminationPoint(terminationPoints);
            phyNodeBuilder.setPhysical(physicalBuilder.build());
            phyNodeBuilder.setNodeId(phyNode.getNodeId());
            phyNodeBuilder.setTopologyRef(new TopologyId(PHY_TOPO_KEY));

            return phyNodeBuilder.build();
        }).collect(Collectors.toList());
        return nmsPhyNodes;
    }

    private List<TerminationPoint> getTerminationPoint(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint> tps) {
        List<TerminationPoint> terminationPoints = new ArrayList<>();
        if (CollectionUtils.isEmpty(tps)) {
            return terminationPoints;
        }
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : tps) {
            TerminationPointBuilder tb = new TerminationPointBuilder(tp);
            tb.fieldsFrom(tp.getAugmentation(TerminationPoint1.class));
            terminationPoints.add(tb.build());
        }

        return terminationPoints;
    }

    private Properties getPhyNodeAdditionalProperties(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node phyNode,
            Map<String, PhyNodeCache> neCacheMap) {
        try {
            PhyNodeCache phyNodeCache = neCacheMap.get(phyNode.getNodeId().getValue());
            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = phyNode.getAugmentation(Node1.class).getPhysical()
                    .getProperties().getProperty();
            PropertyTool.putKeyValue(pList, ADAPTER_ID, phyNodeCache.getAdapter());
            PropertyTool.putKeyValue(pList, COLLECTOR_ID, phyNodeCache.getTelemetryServer());
            PropertyTool.putKeyValue(pList, CURRENT_SOFTWARE_VERSION,
                    phyNodeCache.getSoftwareVersion());

            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }


    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.PHY_NODE;
    }
}
