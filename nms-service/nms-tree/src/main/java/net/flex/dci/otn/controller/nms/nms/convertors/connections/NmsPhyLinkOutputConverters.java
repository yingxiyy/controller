package net.flex.dci.otn.controller.nms.nms.convertors.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DOMAIN_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_NAME;

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
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 10:34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NmsPhyLinkOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> {


    @Override
    public List<Link> convert2NmsOutput(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> linkList) {
        log.debug("convert to nms output for the phy link");
        List<Link> result = new LinkedList<>();
        if (linkList == null || linkList.isEmpty()) {
            return result;
        }
        List<String> phyLinkIds = linkList.stream().map(LinkAttributes::getLinkId).map(
                Uri::getValue).collect(
                Collectors.toList());
        Map<String, PhyLinkCache> phyLinkCacheMap = dciTopologyCacheManager.batchGetValues(
                phyLinkIds, PhyLinkCache.class);
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : linkList) {
//            if (link.getLinkId().getValue().contains("LAG")) {
//                //discard this virtual link.
//                continue;
//            }
            LinkBuilder lb = new LinkBuilder();
            lb.fieldsFrom(link);
            Physical physical = link.getAugmentation(Link1.class).getPhysical();
            PhysicalBuilder physicalBuilder = new PhysicalBuilder(physical);
            physicalBuilder.setProperties(enrichPhyLinkProperties(link, phyLinkCacheMap));
            // for basic info, discard some info.
            lb.setSupportingLink(null);
            lb.setKey(
                    new LinkKey(link.getLinkId(), new TopologyId(PHY_TOPO_KEY)));
            lb.setPhysical(physicalBuilder.build());
            result.add(lb.build());
        }
        return result;
    }

    private Properties enrichPhyLinkProperties(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link,
            Map<String, PhyLinkCache> phyLinkCacheMap) {
        log.debug("enrich Phy Link Properties for link id:{}", link.getLinkId());

        try {
            PhyLinkCache phyLinkCache = phyLinkCacheMap.get(link.getLinkId().getValue());
            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = new LinkedList<>();
            LinkNodeInfo destination = phyLinkCache.getDestination();
            LinkNodeInfo source = phyLinkCache.getSource();

            PropertyTool.putKeyValue(pList, SOURCE_NODE_NAME, source.getNodeName());
            PropertyTool.putKeyValue(pList, SOURCE_TP_NAME, source.getPortName());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_NAME, source.getSiteName());
            PropertyTool.putKeyValue(pList, DOMAIN_NAME, source.getDomainName());
            PropertyTool.putKeyValue(pList, SOURCE_NE_ID, source.getNodeId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_ID, source.getPortId());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_ID, source.getSiteId());

            PropertyTool.putKeyValue(pList, DEST_NE_ID, destination.getNodeId());
            PropertyTool.putKeyValue(pList, DEST_NODE_NAME, destination.getNodeName());
            PropertyTool.putKeyValue(pList, DEST_SITE_ID, destination.getSiteId());
            PropertyTool.putKeyValue(pList, DEST_SITE_NAME, destination.getSiteName());
            PropertyTool.putKeyValue(pList, DEST_TP_NAME, destination.getPortName());
            PropertyTool.putKeyValue(pList, DEST_TP_ID, destination.getPortId());

            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }


    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.PHY_LINK;
    }


}


