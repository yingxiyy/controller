package net.flex.dci.otn.controller.db.monitor.core.additional;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.NODE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PHYSICAL;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/17 16:24
 */
@Component
@Slf4j
public class PhyNodeAdditionalPropertyFactory extends AbstractAdditionalPropertyFactory {


    @Override
    public List<Property> getAdditionalProperty(String id) {
        PhyNodeCache phyNodeCache = dciTopologyCacheManager.getValue(id, PhyNodeCache.class);
        List<Property> properties = new ArrayList<>();
        if (phyNodeCache == null) {
            log.warn("phyNodeCache not found for id:{}", id);
            return new ArrayList<>();
        }
        PhysicalPropertyTool.putKeyValue(properties, PropertiesKeyName.CURRENT_SOFTWARE_VERSION,
                phyNodeCache.getSoftwareVersion());
        PhysicalPropertyTool.putKeyValue(properties, PropertiesKeyName.PROPERTY_TELEMETRY_ID,
                phyNodeCache.getTelemetryServer());
        PhysicalPropertyTool.putKeyValue(properties, PropertiesKeyName.PROPERTY_ADAPTER_ID,
                phyNodeCache.getAdapter());
        PhysicalPropertyTool.putKeyValue(properties, PropertiesKeyName.SITE_NAME,
                phyNodeCache.getSiteName());
        PhysicalPropertyTool.putKeyValue(properties, PropertiesKeyName.SITE_ID,
                phyNodeCache.getSiteId());
        return properties;
    }

    @Override
    public Document addAdditionalProperty(String id, Document document) {
        String objectId = document.getString(NODE_ID);
        List<Property> additionalProperty = getAdditionalProperty(objectId);
        Document mergeDocument = addAdditionalProperty(document, additionalProperty);
        return mergeDocument;
    }


    @Override
    public ObjectType getObjectType() {
        return ObjectType.Node;
    }

    @Override
    protected String getPhysicalKey() {
        return PHYSICAL;
    }
}


