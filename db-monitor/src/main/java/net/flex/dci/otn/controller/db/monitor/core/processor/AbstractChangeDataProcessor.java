package net.flex.dci.otn.controller.db.monitor.core.processor;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TUNNEL_COLLECTION;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.LINK_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PHY_LINK_ATTRIBUTE_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PHY_LINK_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.RouteConstants.EXPLICIT_ROUTE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.SITE_LINK_ATTRIBUTE_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.SITE_LINK_CONTAINER;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.SUPPORTING_LINK;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TUNNEL_CONTAINER;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.dto.Properties;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.core.dto.node.NodeChangeObject;
import net.flex.dci.otn.controller.db.monitor.core.dto.node.Physical;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.DocumentTransferUtils;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/14 14:45
 */
@Slf4j
@Component
@RequiredArgsConstructor
public abstract class AbstractChangeDataProcessor {

    protected final DciTopologyCacheManager dciTopologyCacheManager;

    protected final AdditionalPropertyFactory additionalPropertyFactory;

    public ChangeObject enrichChangeObject(ChangeObject changeObject) {
        return null;
    }

    public List<ChangeObject> processChangeEvent(ChangeObject changeObject) {
        return new ArrayList<>();
    }

    private static final ExecutorService CACHE_CLEANUP_EXECUTOR = new ThreadPoolExecutor(
            2,
            4,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder()
                    .setNameFormat("cache-cleanup-%d")
                    .setDaemon(true)
                    .build(),
            new ThreadPoolExecutor.DiscardPolicy()
    );


    protected void discardCache(List<String> ids) {
        log.debug("discard the cache for the ids:{}", ids);
        CompletableFuture.runAsync(() -> {
            ids.forEach(dciTopologyCacheManager::remove);
        }, CACHE_CLEANUP_EXECUTOR);
    }

    protected void discardCache(String id) {
        log.debug("discard the cache for the id:{}", id);
        CompletableFuture.runAsync(() -> {
            dciTopologyCacheManager.remove(id);
        }, CACHE_CLEANUP_EXECUTOR);
    }

    protected Document buildAdditionPropertyChangeObjectForNe(String key, String value,
            String neId) {
        List<Property> properties = additionalPropertyFactory.getAdditionalProperty(neId);
        PhysicalPropertyTool.putKeyValue(properties, key, value);
        NodeChangeObject nodeChangeObject = NodeChangeObject.builder().nodeId(neId).physical(
                        Physical.builder()
                                .properties(
                                        Properties.builder().property(properties)
                                                .build()).build())
                .build();
        Document document = DocumentTransferUtils.toDocument(nodeChangeObject);
        return document;
    }


    public List<ChangeObject> processUpdateObject(ChangeObject changeObject,
            Document sourceDocument) {
        //process data change update object
        return new ArrayList<>();
    }

    /**
     * build protected site link changed object body
     *
     * @param siteLinkId
     * @return
     */
    protected ChangeObject buildSiteLinkChanged(String siteLinkId, Document siteLink) {
        log.debug("change the site link is:{}", siteLinkId);
        log.info("change the protection state for the site link:{}", siteLinkId);
//        Document siteLink = siteLinkDao.getSiteLinkDocumentById(siteLinkId);
        Document siteLinkChangeBody = getSiteLinkChangedBody(siteLink);
        Document enrichedSiteLinkChangeBody = additionalPropertyFactory.enrichAdditionalProperty(
                siteLinkId, siteLinkChangeBody);
        ChangeObject siteLinkChangeObject = new ChangeObject();
        siteLinkChangeObject.setChangeBody(enrichedSiteLinkChangeBody);
        siteLinkChangeObject.setObjectType(ObjectType.Sitelink.name());
        siteLinkChangeObject.setCollectionName(SITE_LINK_COLLECTION);
        return siteLinkChangeObject;
    }

    protected ChangeObject buildPhyLinkChangeObject(String phyLinkId,
            Document phyLinkDoc) {
        log.debug("update phy link cache and extract change object for phy link:{}",
                phyLinkId);
        log.debug("change the phy link is:{}", phyLinkId);
        Document phyLinkChangeBody = getPhyLinkChangedBody(phyLinkDoc);
        Document enrichedSiteLinkChangeBody = additionalPropertyFactory.enrichAdditionalProperty(
                phyLinkId, phyLinkChangeBody);
        ChangeObject siteLinkChangeObject = new ChangeObject();
        siteLinkChangeObject.setChangeBody(enrichedSiteLinkChangeBody);
        siteLinkChangeObject.setObjectType(ObjectType.Link.name());
        siteLinkChangeObject.setCollectionName(PHY_LINK_COLLECTION);
        return siteLinkChangeObject;

    }

    private Document getPhyLinkChangedBody(Document phyLinkDoc) {
        log.debug("extract the phy link change body:{}", phyLinkDoc);
        Document phyLinkDocument = phyLinkDoc.get(LINK_CONTAINER, Document.class);
        Document phyLinkAttributeDocument = phyLinkDocument.get(PHY_LINK_ATTRIBUTE_CONTAINER,
                Document.class);
        phyLinkDocument.remove(SITE_LINK_ATTRIBUTE_CONTAINER);
        phyLinkAttributeDocument.remove(SUPPORTING_LINK);
        phyLinkDocument.put(PHY_LINK_CONTAINER, phyLinkAttributeDocument);
        return phyLinkDocument;
    }


    /**
     * build protected tunnel changed object body
     *
     * @param tunnelId
     * @return
     */
    protected ChangeObject buildTunnelChanged(String tunnelId, Document tunnel) {
        log.debug("change tunnel is:{}", tunnelId);
        Document tunnelChangeBody = getTunnelChangedBody(tunnel);
        Document enrichedTunnelChangeBody = additionalPropertyFactory.enrichAdditionalProperty(
                tunnelId, tunnelChangeBody);
        ChangeObject tunnelChangeObject = new ChangeObject();
        tunnelChangeObject.setChangeBody(enrichedTunnelChangeBody);
        tunnelChangeObject.setObjectType(ObjectType.Tunnel.name());
        tunnelChangeObject.setCollectionName(TUNNEL_COLLECTION);
        return tunnelChangeObject;
    }

    private Document getSiteLinkChangedBody(Document siteLink) {
        log.debug("extract the site link change body:{}", siteLink);
        Document siteLinkDocument = siteLink.get(LINK_CONTAINER, Document.class);
        Document siteAttributeDocument = siteLinkDocument.get(SITE_LINK_ATTRIBUTE_CONTAINER,
                Document.class);
        siteAttributeDocument.remove(EXPLICIT_ROUTE);
        siteLinkDocument.remove(SITE_LINK_ATTRIBUTE_CONTAINER);
        siteLinkDocument.remove(SUPPORTING_LINK);

        siteLinkDocument.put(SITE_LINK_CONTAINER, siteAttributeDocument);
        return siteLinkDocument;
    }

    private Document getTunnelChangedBody(Document tunnel) {
        log.debug("extract the tunnel changed body :{}", tunnel);
        Document tunnelDocument = tunnel.get(TUNNEL_CONTAINER, Document.class);
        tunnelDocument.remove(EXPLICIT_ROUTE);
        return tunnelDocument;
    }
}
