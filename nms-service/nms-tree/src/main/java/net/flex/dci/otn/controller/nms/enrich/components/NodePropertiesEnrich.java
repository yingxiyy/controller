package net.flex.dci.otn.controller.nms.enrich.components;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_NAME;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/3/21 13:29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NodePropertiesEnrich implements DataObjectPropertiesEnrich {

    private final DciTopologyCacheManager cacheManager;

    @Override
    public DataObject enrichProperties(DataObject dataObject) {
        log.debug("start to enrich the node data");
        Node node = (Node) dataObject;
        DataObject enrichedDataObject = enrichNodeProperties(node);
        return enrichedDataObject;
    }

    private DataObject enrichNodeProperties(Node node) {
        String neId = node.getNodeId().getValue();
        log.debug("start to enrich the node properties for node ,the node id is:{}", neId);
        DataObject richedDataObject = node;
        if (PhysicalNodeIdNamingRule.isPhyNodeId(neId)) {
            richedDataObject = enrichPhyNode(node);
        }
        return richedDataObject;
    }


    /**
     * enrich phy node additional properties
     *
     * @param node
     * @return
     */
    private DataObject enrichPhyNode(Node node) {
        String nodeId = node.getNodeId().getValue();
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        Properties properties = physical.getProperties();
        PhyNodeCache phyNodeCache = cacheManager.getValue(nodeId);
        Properties enrichedProperties = enrichNodeProperties(properties, phyNodeCache);

        NodeBuilder nodeBuilder = new NodeBuilder(node);
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(physical);
        physicalBuilder.setProperties(enrichedProperties);
        nodeBuilder.addAugmentation(Node1.class,
                new Node1Builder(node.getAugmentation(Node1.class)).setPhysical(
                        physicalBuilder.build()).build());
        return nodeBuilder.build();
    }

    private Properties enrichNodeProperties(Properties properties, PhyNodeCache phyNodeCache) {
        try {

            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = new ArrayList<>();
            if (properties != null && properties.getProperty() != null) {
                pList = properties.getProperty();
            }

            PropertyTool.putKeyValue(pList, SITE_NAME, phyNodeCache.getSiteName());
            PropertyTool.putKeyValue(pList, SITE_ID, phyNodeCache.getSiteId());

            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }
}
