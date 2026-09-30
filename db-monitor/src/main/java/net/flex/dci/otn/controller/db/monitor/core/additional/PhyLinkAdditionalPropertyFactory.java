package net.flex.dci.otn.controller.db.monitor.core.additional;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.LINK_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PHYSICAL;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_NE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_SITE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_TP_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_TP_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DOMAIN_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_TP_NAME;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/17 16:25
 */
@Component
@Slf4j
public class PhyLinkAdditionalPropertyFactory extends AbstractAdditionalPropertyFactory {


    @Override
    public Document addAdditionalProperty(String id, Document document) {
        String linkId = document.getString(LINK_ID);
        List<Property> additionalProperty = getAdditionalProperty(linkId);
        Document richDoc = addAdditionalProperty(document, additionalProperty);
        return richDoc;
    }

    @Override
    public List<Property> getAdditionalProperty(String id) {
        PhyLinkCache phyLinkCache = dciTopologyCacheManager.getValue(id, PhyLinkCache.class);
        log.debug("[{}]phyLinkCache is:{}", id, phyLinkCache);
        List<Property> properties = new ArrayList<>();
        LinkNodeInfo source = phyLinkCache.getSource();
        LinkNodeInfo destination = phyLinkCache.getDestination();
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_NODE_NAME, source.getNodeName());
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_TP_NAME, source.getPortName());
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_SITE_NAME, source.getSiteName());
        PhysicalPropertyTool.putKeyValue(properties, DOMAIN_NAME, source.getDomainName());
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_NE_ID, source.getNodeId());
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_TP_ID, source.getPortId());
        PhysicalPropertyTool.putKeyValue(properties, SOURCE_SITE_ID, source.getSiteId());

        PhysicalPropertyTool.putKeyValue(properties, DEST_NE_ID, destination.getNodeId());
        PhysicalPropertyTool.putKeyValue(properties, DEST_NODE_NAME, destination.getNodeName());
        PhysicalPropertyTool.putKeyValue(properties, DEST_SITE_ID, destination.getSiteId());
        PhysicalPropertyTool.putKeyValue(properties, DEST_SITE_NAME, destination.getSiteName());
        PhysicalPropertyTool.putKeyValue(properties, DEST_TP_NAME, destination.getPortName());
        PhysicalPropertyTool.putKeyValue(properties, DEST_TP_ID, destination.getPortId());

        return properties;

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.Link;
    }

    @Override
    protected String getPhysicalKey() {
        return PHYSICAL;
    }
}
