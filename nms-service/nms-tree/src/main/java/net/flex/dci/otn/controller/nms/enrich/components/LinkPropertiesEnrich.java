package net.flex.dci.otn.controller.nms.enrich.components;

import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_NAME;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/1/17 13:29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LinkPropertiesEnrich implements DataObjectPropertiesEnrich {

    private final DciTopologyCacheManager cacheManager;

    @Override
    public DataObject enrichProperties(DataObject dataObject) {
        log.debug("start to enrich the properties for the link data object");
        DataObject richedDataObject = enrichLinkDataObject((Link) dataObject);
        return richedDataObject;
    }

    private DataObject enrichLinkDataObject(Link link) {
        String linkId = link.getLinkId().getValue();
        log.debug("enrich the phy link the link id is :{}", linkId);
        DataObject richedDataObject = link;
        if (PhysicalLinkIdNamingRule.isPhysicalLinkId(linkId)) {
            richedDataObject = enrichPhyLinkProperties(link);
        } else if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
            richedDataObject = enrichSiteLinkProperties(link);
        }
        return richedDataObject;
    }

    private DataObject enrichSiteLinkProperties(Link link) {
        String siteLinkId = link.getLinkId().getValue();
        Site site = link.getAugmentation(Link1.class).getSite();
        Properties properties = site.getProperties();
        SiteLinkCache siteLinkCache = cacheManager.getValue(siteLinkId, SiteLinkCache.class);
        Properties enrichedProperties = enrichLinkProperties(properties, siteLinkCache);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.fieldsFrom(link);
        SiteBuilder siteBuilder = new SiteBuilder();
        siteBuilder.fieldsFrom(site);
        siteBuilder.setLinkGroup(site.getLinkGroup());
        siteBuilder.setBandwidth(site.getBandwidth());
        siteBuilder.setGrid(site.getGrid());
        siteBuilder.setSingleFrequencyPower(site.getSingleFrequencyPower());
        siteBuilder.setProperties(enrichedProperties);
        linkBuilder.addAugmentation(Link1.class,
                new Link1Builder().setSite(siteBuilder.build()).build());

        return linkBuilder.build();
    }

    private DataObject enrichPhyLinkProperties(Link link) {
        String phyLinkId = link.getLinkId().getValue();
        Properties properties = link.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical().getProperties();
        PhyLinkCache phyLinkCache = cacheManager.getValue(phyLinkId, PhyLinkCache.class);
        Properties enrichedProperties = enrichLinkProperties(properties, phyLinkCache);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.fieldsFrom(link);
        PhysicalBuilder physicalBuilder = new PhysicalBuilder();
        Physical phyLinkPhysical = link.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical();
        physicalBuilder.fieldsFrom(phyLinkPhysical);
        physicalBuilder.setLinkType(phyLinkPhysical.getLinkType());
        physicalBuilder.setProperties(enrichedProperties);
        linkBuilder.addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder().setPhysical(
                        physicalBuilder.build()).build());
        return linkBuilder.build();
    }

    private Properties enrichLinkProperties(Properties properties, Object cache) {

        try {

            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = new ArrayList<>();
            if (properties != null && properties.getProperty() != null) {
                pList = properties.getProperty();
            }

            LinkNodeInfo destination = null;
            LinkNodeInfo source = null;
            if (cache instanceof SiteLinkCache) {
                destination = ((SiteLinkCache) cache).getDestination();
                source = ((SiteLinkCache) cache).getSource();
            } else if (cache instanceof PhyLinkCache) {
                destination = ((PhyLinkCache) cache).getDestination();
                source = ((PhyLinkCache) cache).getSource();
            }

            PropertyTool.putKeyValue(pList, SOURCE_NODE_NAME, source.getNodeName());
            PropertyTool.putKeyValue(pList, SOURCE_NE_ID, source.getNodeId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_ID, source.getPortId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_NAME, source.getPortName());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_ID, source.getSiteId());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_NAME, source.getSiteName());

            PropertyTool.putKeyValue(pList, DEST_NODE_NAME, destination.getNodeName());
            PropertyTool.putKeyValue(pList, DEST_NE_ID, destination.getNodeId());
            PropertyTool.putKeyValue(pList, DEST_TP_ID, destination.getPortId());
            PropertyTool.putKeyValue(pList, DEST_TP_NAME, destination.getPortName());
            PropertyTool.putKeyValue(pList, DEST_SITE_NAME, destination.getSiteName());
            PropertyTool.putKeyValue(pList, DEST_SITE_ID, destination.getSiteId());

            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }

}
