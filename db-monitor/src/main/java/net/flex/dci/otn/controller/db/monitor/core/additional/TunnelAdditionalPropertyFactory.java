package net.flex.dci.otn.controller.db.monitor.core.additional;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TUNNEL_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TUNNEL_ROOT;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.AZ_ACTIVE;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.AZ_DELAY;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.CENTRE_FREQUENCY;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_NE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_SITE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_TP_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DEST_TP_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.DOMAIN_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.LEG_REQUIRED;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.SOURCE_TP_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.ZA_ACTIVE;
import static net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName.ZA_DELAY;

import com.alibaba.fastjson.JSON;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.db.monitor.core.dto.Properties;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/17 16:25
 */
@Component
@Slf4j
public class TunnelAdditionalPropertyFactory extends AbstractAdditionalPropertyFactory {

    @Autowired
    private TunnelDao tunnelDao;

    @Override
    public List<Property> getAdditionalProperty(String id) {
        TunnelCache tunnelCache = dciTopologyCacheManager.getValue(id, TunnelCache.class);
        TunnelGeneralInfo tunnelGeneralInfo = getTunnelGeneralInfo(id);
        List<Property> properties = new ArrayList<>();
        if (tunnelCache == null) {
            return properties;
        }
        LinkNodeInfo source = tunnelCache.getSource();
        LinkNodeInfo destination = tunnelCache.getDestination();
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
        PhysicalPropertyTool.putKeyValue(properties, DOMAIN_NAME, tunnelCache.getDomainName());

        PhysicalPropertyTool.putKeyValue(properties, CENTRE_FREQUENCY,
                tunnelGeneralInfo.centerFrequency);
        PhysicalPropertyTool.putKeyValue(properties, LEG_REQUIRED, tunnelGeneralInfo.legRequired);
        PhysicalPropertyTool.putKeyValue(properties, AZ_ACTIVE,
                getApsActivePath(tunnelCache.getDestinationApsXCId()));
        PhysicalPropertyTool.putKeyValue(properties, ZA_ACTIVE,
                getApsActivePath(tunnelCache.getSourceApsXCId()));
        //delay
        String azActive = getApsActivePath(tunnelCache.getDestinationApsXCId());
        String zaActive = getApsActivePath(tunnelCache.getSourceApsXCId());

        PhysicalPropertyTool.putKeyValue(properties, AZ_DELAY,
                getAZDelay(tunnelCache, azActive));
        PhysicalPropertyTool.putKeyValue(properties, ZA_DELAY,
                getZADelay(tunnelCache, zaActive));

        return properties;
    }

    private String getAZDelay(TunnelCache tunnelCache, String azActive) {
        if (azActive.equals(ApsPath.PRIMARY.name())) {
            return tunnelCache.getPrimaryDelay().getAzDelay();
        } else if (azActive.equals(ApsPath.SECONDARY.name())) {
            return tunnelCache.getSecondaryDelay().getAzDelay();
        } else if (azActive.equals(ApsPath.THIRD.name())) {
            return tunnelCache.getTertiaryDelay().getAzDelay();
        }
        return "--";
    }


    private String getZADelay(TunnelCache tunnelCache, String azActive) {
        if (azActive.equals(ApsPath.PRIMARY.name())) {
            return tunnelCache.getPrimaryDelay().getZaDelay();
        } else if (azActive.equals(ApsPath.SECONDARY.name())) {
            return tunnelCache.getSecondaryDelay().getZaDelay();
        } else if (azActive.equals(ApsPath.THIRD.name())) {
            return tunnelCache.getTertiaryDelay().getZaDelay();
        }
        return "--";
    }

    private TunnelGeneralInfo getTunnelGeneralInfo(String id) {
        Document tunnelDoc = tunnelDao.getTunnelDocumentById(id);
        log.debug("current tunnelDoc is:{}", tunnelDoc);
        Document tunnel = tunnelDoc.get(TUNNEL_CONTAINER, Document.class);
        Document properties = (Document) ((Document) tunnel).get(PROPERTIES);
        String propertiesJson = properties.toJson(JsonWriterSettings.builder().build());
        Properties propertyList = JSON.parseObject(propertiesJson, Properties.class);
        String centreFrequencyValue = propertyList.getProperty().stream()
                .filter(prop -> CENTRE_FREQUENCY.equals(prop.getName()))
                .map(Property::getValue)
                .findFirst()
                .orElse(null);
        String isLegRequiredStr = propertyList.getProperty().stream()
                .filter(prop -> LEG_REQUIRED.equals(prop.getName()))
                .map(Property::getValue)
                .findFirst()
                .orElse(null);

        return TunnelGeneralInfo.builder().centerFrequency(centreFrequencyValue)
                .legRequired(isLegRequiredStr).build();
    }


    @Override
    public Document addAdditionalProperty(String id, Document document) {
//        String linkId = document.getString(TUNNEL_ID);
        log.debug("add additional property to the tunnel document for the id:{} document:{}", id,
                document);
        List<Property> additionalProperty = getAdditionalProperty(id);
        Document richDoc = addTunnelAdditionalProperty(document, additionalProperty);
        return richDoc;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.Tunnel;
    }

    @Override
    protected String getPhysicalKey() {
        return TUNNEL_ROOT;
    }


    protected Document addTunnelAdditionalProperty(Document document,
            List<Property> additionalProperty) {
        Document properties = (Document) ((Document) document).get(PROPERTIES);
        if (properties == null) {
            Properties nProperties = Properties.builder().property(additionalProperty).build();
            Document nPropertyDocument = Document.parse(
                    JSON.toJSONString(nProperties));
            ((Document) document).put(PROPERTIES, nPropertyDocument);
        } else {
            String propertiesJson = properties.toJson(JsonWriterSettings.builder().build());
            Properties propertyList = JSON.parseObject(propertiesJson, Properties.class);
            List<Property> mergeProperty = PhysicalPropertyTool.mergeProperty(
                    propertyList.getProperty(), additionalProperty);
            Properties nProperties = Properties.builder().property(mergeProperty).build();
            Document nPropertyDocument = Document.parse(
                    JSON.toJSONString(nProperties));
            ((Document) document).put(PROPERTIES, nPropertyDocument);
        }
        return document;
    }

    @Data
    @Builder
    private static class TunnelGeneralInfo implements Serializable {

        private String centerFrequency;

        private String legRequired;
    }
}
