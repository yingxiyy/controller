package net.flex.dci.otn.controller.nms.nms.convertors.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.AZ_ACTIVE;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DOMAIN_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.ZA_ACTIVE;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 10:34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NmsSiteLinkOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> {


    @Override
    public List<Link> convert2NmsOutput(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks) {
        log.debug("start to convert the site link");
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> output = new LinkedList<>();
        if (siteLinks == null || siteLinks.isEmpty()) {
            return output;
        }
        List<String> siteLinkIds = siteLinks.stream().map(LinkAttributes::getLinkId).map(
                Uri::getValue).collect(
                Collectors.toList());
        Map<String, SiteLinkCache> siteLinkCacheMap = dciTopologyCacheManager.batchGetValues(
                siteLinkIds, SiteLinkCache.class);
        siteLinks.forEach(siteLink -> {
            LinkBuilder lb = new LinkBuilder();
            lb.fieldsFrom(siteLink);
            Site site = siteLink.getAugmentation(Link1.class).getSite();
            SiteBuilder sb = new SiteBuilder(site);
            sb.setDummyLink(null);
            lb.setSupportingLink(null);
            sb.setAvailable(null);
            sb.setExplictRoute(null);
            sb.setAExternal(null);
            sb.setZExternal(null);
            sb.setSupportedLink(null);
            sb.setProperties(enrichSiteLinkProperties(siteLink, siteLinkCacheMap));
            lb.setSite(sb.build());
            lb.setKey(new LinkKey(siteLink.getLinkId(), new TopologyId(SITE_TOPO_KEY)));
            output.add(lb.build());
        });
        return output;
    }

    private Properties enrichSiteLinkProperties(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link,
            Map<String, SiteLinkCache> siteLinkCacheMap) {
        Site siteLinkPhysical = link.getAugmentation(
                Link1.class).getSite();
        Properties properties = siteLinkPhysical.getProperties();
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder(properties);
        List<Property> propertyList = propertiesBuilder.getProperty();
        SiteLinkCache siteLinkCache = siteLinkCacheMap.get(link.getLinkId().getValue());

        LinkNodeInfo source = siteLinkCache.getSource();
        LinkNodeInfo destination = siteLinkCache.getDestination();

        PropertyTool.putKeyValue(propertyList, SOURCE_NODE_NAME, source.getNodeName());
        PropertyTool.putKeyValue(propertyList, SOURCE_TP_NAME, source.getPortName());
        PropertyTool.putKeyValue(propertyList, SOURCE_SITE_NAME, source.getSiteName());
        PropertyTool.putKeyValue(propertyList, DOMAIN_NAME, source.getDomainName());
        PropertyTool.putKeyValue(propertyList, SOURCE_NE_ID, source.getNodeId());
        PropertyTool.putKeyValue(propertyList, SOURCE_TP_ID, source.getPortId());
        PropertyTool.putKeyValue(propertyList, SOURCE_SITE_ID, source.getSiteId());

        PropertyTool.putKeyValue(propertyList, DEST_NE_ID, destination.getNodeId());
        PropertyTool.putKeyValue(propertyList, DEST_NODE_NAME, destination.getNodeName());
        PropertyTool.putKeyValue(propertyList, DEST_SITE_ID, destination.getSiteId());
        PropertyTool.putKeyValue(propertyList, DEST_SITE_NAME, destination.getSiteName());
        PropertyTool.putKeyValue(propertyList, DEST_TP_NAME, destination.getPortName());
        PropertyTool.putKeyValue(propertyList, DEST_TP_ID, destination.getPortId());
        //active za az active
        PropertyTool.putKeyValue(propertyList, AZ_ACTIVE,
                getApsActivePath(siteLinkCache.getDestinationApsXCId()));
        PropertyTool.putKeyValue(propertyList, ZA_ACTIVE,
                getApsActivePath(siteLinkCache.getSourceApsXCId()));

        propertiesBuilder.setProperty(propertyList);
        return propertiesBuilder.build();
    }


    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.SITE_LINK;
    }


}
