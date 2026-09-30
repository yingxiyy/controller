package net.flex.dci.otn.controller.nms.nms.convertors.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.AZ_ACTIVE;
import static net.flex.dci.otn.controller.nms.utils.Constants.AZ_DELAY;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DOMAIN_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.ZA_ACTIVE;
import static net.flex.dci.otn.controller.nms.utils.Constants.ZA_DELAY;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.dto.TunnelDelayDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.TunnelBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.TunnelKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 10:33
 */
@Slf4j
@Component
public class NmsTunnelOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel, Tunnel> {

    @Autowired
    private PhyLinkDao phyLinkDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private SiteLinkDao siteLinkDao;

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel> convert2NmsOutput(
            List<Tunnel> tunnelList) {
        log.debug("convert nms tunnel output ");
        long startTime = System.currentTimeMillis();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel> tunnels = new LinkedList<>();
        if (tunnelList == null || tunnelList.isEmpty()) {
            return tunnels;
        }
        List<String> tunnelIds = tunnelList.stream().map(TunnelBaseAttributes::getTunnelId).map(
                Uri::getValue).collect(
                Collectors.toList());
//        Map<String, TunnelCache> tunnelCacheMap = dciTopologyCacheManager.batchGetValues(tunnelIds,
//                TunnelCache.class);
        long t2 = System.currentTimeMillis();
        Map<String, TunnelCache> tunnelCacheMap = dciTopologyCacheManager.batchGetValues(tunnelIds,
                TunnelCache.class);
        log.info("get tunnelCacheMap cost:{}ms", System.currentTimeMillis() - t2);
//        tunnelList.forEach(tunnel -> {
//            TunnelBuilder tunnelBuilder = new TunnelBuilder();
//            tunnelBuilder.fieldsFrom(tunnel);
//            tunnelBuilder.setSupportingLink(null);
//            tunnelBuilder.setExplictRoute(null);
//            tunnelBuilder.setKey(new TunnelKey(tunnel.getTunnelId()));
//            tunnelBuilder.setProperties(enrichTunnelProperties(tunnel, tunnelCacheMap));
//            tunnels.add(tunnelBuilder.build());
//        });
        long t3 = System.currentTimeMillis();
        tunnelList.forEach(tunnel -> {
            long tunnelStart = System.currentTimeMillis();
            TunnelBuilder tunnelBuilder = new TunnelBuilder();
            tunnelBuilder.fieldsFrom(tunnel);
            tunnelBuilder.setSupportingLink(null);
            tunnelBuilder.setExplictRoute(null);
            tunnelBuilder.setKey(new TunnelKey(tunnel.getTunnelId()));
            tunnelBuilder.setProperties(enrichTunnelProperties(tunnel, tunnelCacheMap));
            tunnels.add(tunnelBuilder.build());
            log.info("single tunnel convert cost:{}ms", System.currentTimeMillis() - tunnelStart);
        });
        log.info("process all tunnels cost:{}ms", System.currentTimeMillis() - t3);
        log.info("data convert cost:{}ms", System.currentTimeMillis() - startTime);
        return tunnels;
    }

    private Properties enrichTunnelProperties(Tunnel tunnel,
            Map<String, TunnelCache> tunnelCacheMap) {
        log.debug("enrich Phy Link Properties for tunnel id:{}", tunnel.getTunnelId().getValue());

        try {
            TunnelCache tunnelCache = tunnelCacheMap.get(tunnel.getTunnelId().getValue());
            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = tunnel.getProperties().getProperty();
            if (pList == null) {
                pList = new ArrayList<>();
            }
            LinkNodeInfo destination = tunnelCache.getDestination();
            LinkNodeInfo source = tunnelCache.getSource();

            PropertyTool.putKeyValue(pList, SOURCE_NODE_NAME, source.getNodeName());
            PropertyTool.putKeyValue(pList, SOURCE_NE_ID, source.getNodeId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_ID, source.getPortId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_NAME, source.getPortName());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_NAME, source.getSiteName());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_ID, source.getSiteId());

            PropertyTool.putKeyValue(pList, DEST_NODE_NAME, destination.getNodeName());
            PropertyTool.putKeyValue(pList, DEST_NE_ID, destination.getNodeId());
            PropertyTool.putKeyValue(pList, DEST_TP_ID, destination.getPortId());
            PropertyTool.putKeyValue(pList, DEST_TP_NAME, destination.getPortName());
            PropertyTool.putKeyValue(pList, DEST_SITE_ID, destination.getSiteId());
            PropertyTool.putKeyValue(pList, DEST_SITE_NAME, destination.getSiteName());

            PropertyTool.putKeyValue(pList, DOMAIN_NAME, tunnelCache.getDomainName());
            String azActive = getApsActivePath(tunnelCache.getDestinationApsXCId());
            String zaActive = getApsActivePath(tunnelCache.getSourceApsXCId());
            //additional properties
            PropertyTool.putKeyValue(pList, AZ_ACTIVE,
                    azActive);
            PropertyTool.putKeyValue(pList, ZA_ACTIVE,
                    zaActive);

            PropertyTool.putKeyValue(pList, AZ_DELAY,
                    getAZDelay(tunnelCache, azActive));
            PropertyTool.putKeyValue(pList, ZA_DELAY,
                    getZADelay(tunnelCache, zaActive));
            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }

    private String getAZDelay(TunnelCache tunnelCache, String azActive) {
        if (azActive.equals(ApsPath.PRIMARY.name())) {
            return tunnelCache.getPrimaryDelay() == null ? "--"
                    : tunnelCache.getPrimaryDelay().getAzDelay();
        } else if (azActive.equals(ApsPath.SECONDARY.name())) {
            return tunnelCache.getSecondaryDelay() == null ? "--"
                    : tunnelCache.getSecondaryDelay().getAzDelay();
        } else if (azActive.equals(ApsPath.THIRD.name())) {
            return tunnelCache.getTertiaryDelay() == null ? "--"
                    : tunnelCache.getTertiaryDelay().getAzDelay();
        }
        return "--";
    }


    private String getZADelay(TunnelCache tunnelCache, String azActive) {
        if (azActive.equals(ApsPath.PRIMARY.name())) {
            return tunnelCache.getPrimaryDelay() == null ? "--"
                    : tunnelCache.getPrimaryDelay().getZaDelay();
        } else if (azActive.equals(ApsPath.SECONDARY.name())) {
            return tunnelCache.getSecondaryDelay() == null ? "--"
                    : tunnelCache.getSecondaryDelay().getZaDelay();
        } else if (azActive.equals(ApsPath.THIRD.name())) {
            return tunnelCache.getTertiaryDelay() == null ? "--"
                    : tunnelCache.getTertiaryDelay().getZaDelay();
        }
        return "--";
    }

    /**
     * get tunnel
     *
     * @param tunnel
     * @return
     */
    private TunnelDelayDto getTunnelDelay(Tunnel tunnel) {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("get tunnel:{} current delay", tunnelId);
        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        List<String> siteLinkIds = ochLink.getSupportingLink().stream()
                .map(SupportingLink::getLinkRef).map(Uri::getValue)
                .collect(Collectors.toList());
        List<Link> siteLinks = siteLinkDao.listAllSiteLinkByIds(siteLinkIds);
        List<String> otsLinkIds = siteLinks.stream()
                .flatMap(siteLink -> siteLink.getSupportingLink().stream())
                .map(SupportingLink::getLinkRef)
                .map(Uri::getValue)
                .filter(PhysicalLinkIdNamingRule::isOtsLink)
                .collect(Collectors.toList());
        List<Link> otsLinks = phyLinkDao.getAllPhyLinksByIds(otsLinkIds);
        AtomicReference<BigDecimal> azDelay = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> zaDelay = new AtomicReference<>(BigDecimal.ZERO);
        otsLinks.forEach(link -> {
            Physical physical = link.getAugmentation(Link1.class).getPhysical();
            Provider otsLinkProvider = physical.getProvider();
            azDelay.set(azDelay.get().add(otsLinkProvider.getDelayAz()));
            zaDelay.set(zaDelay.get().add(otsLinkProvider.getDelayZa()));
        });
        return TunnelDelayDto.builder().azDelay(azDelay.get()).zaDelay(zaDelay.get()).build();
    }


    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.TUNNEL;
    }
}
