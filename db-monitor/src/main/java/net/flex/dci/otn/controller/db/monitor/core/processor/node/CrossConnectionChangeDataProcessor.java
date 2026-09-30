package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.CrossConnectionConstants.APS_ACTIVE_PATH;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.CrossConnectionConstants.APS_CONTAINER_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.CrossConnectionConstants.CROSS_CONNECTION_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.CrossConnectionConstants.NODE_REF;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.ApsXCCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.OchLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.CrossConnectionRouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 8/25/2025 11:28 AM
 */
@Component
@Slf4j
public class CrossConnectionChangeDataProcessor extends AbstractChangeDataProcessor {

    private final PhyNodeDao phyNodeDao;

    private final TunnelDao tunnelDao;

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;

    public CrossConnectionChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory, PhyNodeDao phyNodeDao,
            TunnelDao tunnelDao, SiteLinkDao siteLinkDao, OchLinkDao ochLinkDao) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
        this.phyNodeDao = phyNodeDao;
        this.tunnelDao = tunnelDao;
        this.siteLinkDao = siteLinkDao;
        this.ochLinkDao = ochLinkDao;
    }

    /**
     * only handle aps change event
     *
     * @param changeObject
     */
    @Override
    public List<ChangeObject> processChangeEvent(ChangeObject changeObject) {
        Document changeData = changeObject.getChangeBody();
        String neId = changeData.getString(NODE_REF);
        String crossConnectionId = changeData.getString(CROSS_CONNECTION_ID);
        log.debug("process the neId:{} cross connection :{} aps change", neId,
                crossConnectionId);
        if (!isApsSwitchEvent(changeData)) {
            dciTopologyCacheManager.removeAsync(crossConnectionId);
            log.debug("the cross connection change data event is not aps switch event,do nothing");
            return Collections.singletonList(changeObject);
        }
        List<ChangeObject> changeObjectList = new LinkedList<>();
        changeObjectList.add(changeObject);
        String activePath = getApsActivePath(changeData, crossConnectionId);
        //todo:get the cross connection relative siteLink or Tunnel
        ChangeObject relativeLinkApsChangeObject = refreshTheRelativeLinkApsState(neId,
                crossConnectionId,
                activePath);
        if (relativeLinkApsChangeObject != null) {
            changeObjectList.add(relativeLinkApsChangeObject);
        }
        return changeObjectList;
    }

    /**
     * get current aps active path
     *
     * @param changeData
     * @return
     */
    private String getApsActivePath(Document changeData, String crossConnectionId) {
        log.debug("get cross connection :{} aps active path ", crossConnectionId);
        Document apsDocument = changeData.get(APS_CONTAINER_KEY, Document.class);
        String activePath = apsDocument.getString(APS_ACTIVE_PATH);
        return activePath;
    }

    /**
     * refresh the relative link aps state
     *
     * @param neId
     * @param crossConnectionId
     */
    private ChangeObject refreshTheRelativeLinkApsState(String neId, String crossConnectionId,
            String activePath) {
        log.debug(
                "refresh the aps connection relative link aps state crossConnectionId:{} and neId :{}",
                crossConnectionId, neId);
        updateApsXCCache(crossConnectionId, activePath);
        Node node = phyNodeDao.getPhyNodeById(neId);
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = nodePhysical.getNodeType();
        ChangeObject changeObject = null;
        if (nodeType == NodeType.TD) {
            changeObject = refreshTheProtectedTunnelApsState(node, crossConnectionId);
        } else if (nodeType == NodeType.OD) {
            changeObject = refreshTheProtectedSiteLinkApsState(node, crossConnectionId);
        }
        return changeObject;
    }

    private void updateApsXCCache(String crossConnectionId, String activePath) {
        log.debug("update aps cross connection :{} current active path:{}", crossConnectionId,
                activePath);
        ApsXCCache apsXCCache = dciTopologyCacheManager.getValue(crossConnectionId);
        apsXCCache.setActivePath(activePath);
        dciTopologyCacheManager.updateValue(crossConnectionId, apsXCCache);
    }

    private ChangeObject refreshTheProtectedSiteLinkApsState(Node node, String crossConnectionId) {
        log.debug("refresh the protected site link aps state");
        List<SiteLinkCache> siteLinkCaches = dciTopologyCacheManager.getValues(SiteLinkCache.class);
        SiteLinkCache refSiteLinkCache = null;
        List<SiteLinkCache> refSiteLinkInfo = siteLinkCaches.stream()
                .filter(siteLinkCache -> siteLinkCache.getSourceApsXCId() != null
                        && siteLinkCache.getDestinationApsXCId() != null)
                .filter(siteLinkCache -> {
                    log.debug("siteLinkCache is:{} refCrossConnectionId is:{}", siteLinkCache,
                            crossConnectionId);
                    return crossConnectionId.startsWith(siteLinkCache.getDestinationApsXCId())
                            || crossConnectionId.startsWith(siteLinkCache.getSourceApsXCId());
                })
                .collect(Collectors.toList());
        if (refSiteLinkInfo.isEmpty()) {
            log.warn("there is no site link for the aps cross connection:{}", crossConnectionId);
            Link refSiteLink = getRefProtectionSiteLink(node, crossConnectionId);
            refSiteLinkCache = refSiteLink == null ? null
                    : dciTopologyCacheManager.getValue(refSiteLink.getLinkId().getValue());
        } else {
            refSiteLinkCache = refSiteLinkInfo.get(0);
        }
        if (refSiteLinkCache == null) {
            log.warn("current node:{} and cross connection :{} have no site link,do nothing",
                    node.getNodeId(), crossConnectionId);
            return null;
        }
        String siteLinkId = refSiteLinkCache.getId();
        Document siteLinkDoc = siteLinkDao.getSiteLinkDocumentById(siteLinkId);
        ChangeObject siteLinkChangeObject = buildSiteLinkChanged(siteLinkId, siteLinkDoc);
        return siteLinkChangeObject;
    }

    private Link getRefProtectionSiteLink(Node node, String crossConnectionId) {
        log.debug("get node:{} and cross connection ref protection site link:{}", node.getNodeId(),
                crossConnectionId);
        List<Link> siteLinks = siteLinkDao.queryWithNode(node.getNodeId().getValue())
                .stream()
                .filter(link -> !link.getAugmentation(Link1.class).getSite().getProtectionType()
                        .isAssignableFrom(
                                ProtectionUnprotected.class))
                .collect(Collectors.toList());
        Link refSiteLink = siteLinks.stream()
                .filter(siteLink -> extractSiteLinkFirstCrossConnectionId(siteLink)
                        .map(crossConnectionId::startsWith)
                        .orElse(false))
                .findFirst()
                .orElse(null);
        return refSiteLink;
    }

    private Tunnel getRefProtectionTunnel(Node node, String crossConnectionId) {
        log.debug("get node:{} and crossConnection:{} ref protection tunnel", node.getNodeId(),
                crossConnectionId);
        List<Link> ochLinks = ochLinkDao.queryWithNode(node.getNodeId().getValue())
                .stream()
                .filter(link -> !link.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch().getProtectionType()
                        .isAssignableFrom(ProtectionUnprotected.class))
                .collect(Collectors.toList());
        Link refOchLink = ochLinks.stream()
                .filter(link -> extractOchLinkFirstCrossConnectionId(link)
                        .map(crossConnectionId::startsWith)
                        .orElse(false))
                .findFirst()
                .orElse(null);
        if (refOchLink == null) {
            return null;
        }
        List<Tunnel> tunnel = tunnelDao.getAllTunnelsUnderOchLink(
                Collections.singletonList(refOchLink.getLinkId().getValue()));
        return tunnel.get(0);
    }

    private Optional<String> extractSiteLinkFirstCrossConnectionId(Link siteLink) {
        return Optional.ofNullable(siteLink.getAugmentation(Link1.class))
                .map(SiteLinkAttributes::getSite)
                .map(ExplictRoute::getExplictRoute)
                .map(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute::getRoute)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(Route::getPrimary)
                .map(CrossConnectionRouteSequence::getCrossConnections)
                .orElse(Collections.emptyList())
                .stream()
                .filter(xc -> xc.getAps() != null)
                .map(CrossConnectionAttributes::getCrossConnectionId)
                .map(Uri::getValue)
                .findFirst();
    }

    private Optional<String> extractOchLinkFirstCrossConnectionId(Link ochLink) {
        return Optional.ofNullable(ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class))
                .map(OchLinkAttributes::getOch)
                .map(ExplictRoute::getExplictRoute)
                .map(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute::getRoute)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(Route::getPrimary)
                .map(CrossConnectionRouteSequence::getCrossConnections)
                .orElse(Collections.emptyList())
                .stream()
                .filter(xc -> xc.getAps() != null)
                .map(CrossConnectionAttributes::getCrossConnectionId)
                .map(Uri::getValue)
                .findFirst();
    }


    private ChangeObject refreshTheProtectedTunnelApsState(Node node, String crossConnectionId) {
        log.debug("refresh neId:{} and aps cross connection:{} the protected tunnel aps state",
                node.getNodeId(), crossConnectionId);
        List<TunnelCache> tunnelCaches = dciTopologyCacheManager.getValues(TunnelCache.class);
        //refresh and send tunnel update method
        List<TunnelCache> refTunnelCacheInfo = tunnelCaches.stream()
                .filter(tunnelCache -> tunnelCache.getDestinationApsXCId() != null
                        && tunnelCache.getSourceApsXCId() != null)
                .filter(tunnelCache -> tunnelCache.getSourceApsXCId().equals(crossConnectionId)
                        || tunnelCache.getDestinationApsXCId().equals(crossConnectionId)).collect(
                        Collectors.toList());
        TunnelCache tunnelCache = null;
        if (refTunnelCacheInfo.isEmpty()) {
            //todo: do nothing or find real one
            log.warn("there is no tunnel for the aps cross connection:{}", crossConnectionId);
            Tunnel tunnel = getRefProtectionTunnel(node, crossConnectionId);
            tunnelCache = tunnel == null ? null
                    : dciTopologyCacheManager.getValue(tunnel.getTunnelId().getValue());
        } else {
            tunnelCache = refTunnelCacheInfo.get(0);
        }

        if (tunnelCache == null) {
            log.warn("current node:{} and cross connection :{} have ref tunnel,do nothing",
                    node.getNodeId(), crossConnectionId);
            return null;
        }
        //only have on tunnel Cache

        String tunnelId = tunnelCache.getId();
        log.debug("the refresh the tunnel :{} change by crossConnection id:{}", tunnelId,
                crossConnectionId);
        //to send notification
        Document tunnelDocument = tunnelDao.getTunnelDocumentById(tunnelId);
        ChangeObject tunnelChangeObject = buildTunnelChanged(tunnelId, tunnelDocument);
        return tunnelChangeObject;
    }


    /**
     * detective the cross connection is aps switch event or not
     *
     * @param changeData
     * @return
     */
    private boolean isApsSwitchEvent(Document changeData) {
        log.debug("detecting  current cross connection change event is aps switch event or not");
        return changeData.containsKey(APS_CONTAINER_KEY);
    }
}
