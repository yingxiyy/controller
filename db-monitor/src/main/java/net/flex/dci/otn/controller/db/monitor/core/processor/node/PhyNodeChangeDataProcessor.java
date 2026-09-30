package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ATTRIBUTE_IP;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.CACHE_RELEVANT_KEYS;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.FRIENDLY_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.IMPLEMENT_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.NE_NOTIFY_TYPE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.ON_IMPLEMENT_STATE_DIRECT_PATH;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.ON_IMPLEMENT_STATE_LEVEL_SECONDARY_PATH;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.ON_IMPLEMENT_STATE_LEVEL_TOP_NUMBER_PATH;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ImplementMonitor.PHY_NODE_PHYSICAL;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.NODE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ADMIN_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ALIGNMENT_STATUS;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.OPERATIONAL_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.PHY_PHYSICAL_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.StatusChangeObjectType;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.dto.NeStateDto;
import net.flex.dci.otn.controller.db.monitor.core.dto.NodeChangeNotifyBody;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.tools.kafka.service.NeConnStatusNotifier;
import net.flex.dci.otn.controller.tools.kafka.service.StatusEventNotifier;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/11/14 15:09
 */
@Component
@Slf4j
public class PhyNodeChangeDataProcessor extends AbstractChangeDataProcessor {

    private static final String SOURCE_NE_KEY = "neId";

    private final PhyLinkDao phyLinkDao;

    private final TunnelDao tunnelDao;

    private final SiteLinkDao siteLinkDao;

    private final Map<String, NodeStateSnapshot> stateCache = new ConcurrentHashMap<>();

    private Thread cleanupThread;


    private volatile boolean running = true;

    private final PhyNodeNameChangeQueueProcessor phyNodeNameChangeQueueProcessor;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NodeStateSnapshot {

        String operState;
        String adminState;
        String alignment;
        long timestamp;
        String softwareVersion;
    }

    @PostConstruct
    public void startCleanupThread() {
        cleanupThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(60000);
                    cleanupExpiredCache();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "phy-node-cache-cleaner");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        log.info("PhyNode state cache cleanup thread started");
    }

    @PreDestroy
    public void stopCleanupThread() {
        running = false;
        if (cleanupThread != null) {
            cleanupThread.interrupt();
        }
        log.info("PhyNode state cache cleanup thread stopped");
    }

    private void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        long ttlMillis = 5 * 60 * 1000;

        stateCache.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().getTimestamp();
            return age > ttlMillis;
        });
    }

    public PhyNodeChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory, PhyLinkDao phyLinkDao,
            TunnelDao tunnelDao, SiteLinkDao siteLinkDao,
            PhyNodeNameChangeQueueProcessor phyNodeNameChangeQueueProcessor) {
        super(dciTopologyCacheManager,
                additionalPropertyFactory);
        this.phyLinkDao = phyLinkDao;
        this.tunnelDao = tunnelDao;
        this.siteLinkDao = siteLinkDao;
        this.phyNodeNameChangeQueueProcessor = phyNodeNameChangeQueueProcessor;
    }

    /**
     * enrich the change object for the ne Id add additional property
     *
     * @param changeObject
     * @return
     */
    @Override
    public ChangeObject enrichChangeObject(ChangeObject changeObject) {
        log.debug("enrich the change object for the phy node");
        Document document = changeObject.getChangeBody();
        String neId = changeObject.getNeId();
        Document richDoc = additionalPropertyFactory.enrichAdditionalProperty(neId, document);
        changeObject.setChangeBody(richDoc);
        return changeObject;
    }

    public void processUnsetAttribute(Document sourceDocument, String unsetAttribute) {
        log.debug("back ground refresh the ip for the node");
        String neId = sourceDocument.getString(SOURCE_NE_KEY);
        if (unsetAttribute.equals(ATTRIBUTE_IP)) {
            dciTopologyCacheManager.removeAsync(neId);
        }
    }


    /**
     * refresh the ne cache info
     *
     * @param changeObject
     */
    public List<ChangeObject> processUpdateObject(ChangeObject changeObject) {
        log.debug("back ground refresh the ip for the node");
        String neId = changeObject.getChangeBody().getString(NODE_ID);
        PhyNodeCache nodeCache = dciTopologyCacheManager.getValue(neId, PhyNodeCache.class);
        if (nodeCache != null) {
            Document nePhysical = changeObject.getChangeBody()
                    .get("physical", Document.class);
            if (nePhysical == null) {
                return Collections.emptyList();
            }
            if (nePhysical.containsKey(FRIENDLY_NAME)) {
//                String newFriendlyName = nePhysical.getString(FRIENDLY_NAME);
                nodeCache.setFriendlyName(nePhysical.getString(FRIENDLY_NAME));
            }
            if (nePhysical.containsKey("properties")) {
                Object propsObj = nePhysical.get("properties");
                if (propsObj instanceof Document) {
                    Map<String, String> property = parseProperty((Document) propsObj);
                    if (property.containsKey("software-version")) {
                        nodeCache.setSoftwareVersion(property.get("software-version"));
                    }
                }
            }
//            if (nePhysical.containsKey("software-version")) nodeCache.setSoftwareVersion(...);
//            if (nePhysical.containsKey("node-type")) nodeCache.setPhyNodeType(...);
            dciTopologyCacheManager.updateValue(neId, nodeCache);
        }
        detectAndFireNodeStatusChange(neId, changeObject.getChangeBody());
        detectAndFireNodeSoftwareVersionChange(neId, changeObject.getChangeBody());
        phyNodeNameChangeQueueProcessor.submitNodeNameChange(neId,
                changeObject.getChangeBody());

        return Collections.singletonList(changeObject);
    }

    private Map<String, String> parseProperty(Document properties) {
        Object propObj = properties.get("property");
        if (propObj == null) {
            return Collections.emptyMap();
        }
        List<Document> propertyList;
        if (propObj instanceof List) {
            propertyList = (List<Document>) propObj;
        } else if (propObj instanceof Document) {
            propertyList = Collections.singletonList((Document) propObj);
        } else {
            return Collections.emptyMap();
        }
        Map<String, String> pros = new HashMap<>();
        for (Document pro : propertyList) {
            String key = pro.getString("name");
            if (key != null) {
                pros.put(key, pro.getString("value"));
            }
        }
        return pros;
    }

    private void detectAndFireNodeSoftwareVersionChange(String neId, Document changeBody) {
        log.debug("detect and fire node:{} software version change ", neId);
        Document nePhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (nePhysical == null) {
            log.debug("node :{} physical attribute nothing change discard software version check",
                    neId);
            return;
        }
        String newVersion = extractSoftwareVersion(nePhysical);
        if (null == newVersion) {
            log.debug("node:{} software nothing change discard software version check", neId);
            return;
        }

//        updateCurrentPhyNodeSoftwareversion(neId, newVersion);
    }

    private String extractSoftwareVersion(Document nePhysical) {
        Object propsObj = nePhysical.get("properties");
        if (!(propsObj instanceof Document)) {
            return null;
        }
        Map<String, String> property = parseProperty((Document) propsObj);
        return property.get("software-version");

    }

    private boolean isCacheRelevantChange(Document changeBody) {
        Document nePhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (nePhysical == null) {
            return false;
        }
        return nePhysical.keySet().stream().anyMatch(CACHE_RELEVANT_KEYS::contains);
    }

    /**
     * +
     *
     * @param neId
     * @param changeBody
     */
    private List<ChangeObject> detectAndFireNodeNameChange(String neId, Document changeBody) {
        log.debug("detect and fire node:{} friendlyName change", neId);
        Document nePhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (nePhysical == null) {
            log.warn("node:{} physical attribute nothing change,discard it", neId);
            return new ArrayList<>();
        }
        String friendlyName =
                nePhysical.containsKey(FRIENDLY_NAME) ? nePhysical.getString(FRIENDLY_NAME) : null;
        if (friendlyName == null) {
            log.warn("neName do not change discard it");
            return new ArrayList<>();
        }
        List<ChangeObject> refPhyLinkChangeObjects = getRefPhyLinkChangeObjects(neId);
        List<ChangeObject> refSiteLinkChangeObjects = getRefSiteLinkChangeObjects(neId);
        List<ChangeObject> refTunnelChangeObjects = getRefTunnelChangeObjects(neId);
        return Stream.of(refTunnelChangeObjects, refSiteLinkChangeObjects, refPhyLinkChangeObjects)
                .flatMap(List::stream).collect(
                        Collectors.toList());
    }

    private List<ChangeObject> getRefTunnelChangeObjects(String neId) {
        log.debug("get Ref tunnel change object neId:{}", neId);
        List<Tunnel> tunnels = tunnelDao.queryWithNode(neId);
        List<String> tunnelIds = tunnels.stream().map(TunnelBaseAttributes::getTunnelId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        List<ChangeObject> tunnelChangeObjects = new ArrayList<>();
        for (String tunnelId : tunnelIds) {
            dciTopologyCacheManager.remove(tunnelId);
            Document tunnelDoc = tunnelDao.getTunnelDocumentById(tunnelId);
            ChangeObject siteChangeObject = buildTunnelChanged(tunnelId, tunnelDoc);
            tunnelChangeObjects.add(siteChangeObject);
        }
        return tunnelChangeObjects;
    }

    private List<ChangeObject> getRefSiteLinkChangeObjects(String neId) {
        log.debug("get Ref site link change object neId:{} ", neId);
        List<Link> siteLinks = siteLinkDao.getSiteLinksEndWithSite(neId);
        List<String> siteLinkIds = siteLinks.stream().map(LinkAttributes::getLinkId).map(
                Uri::getValue).collect(
                Collectors.toList());
        List<ChangeObject> siteLinkChangeObjects = new ArrayList<>();
        for (String siteLinkId : siteLinkIds) {
            dciTopologyCacheManager.remove(siteLinkId);
            Document siteLinkDoc = siteLinkDao.getSiteLinkDocumentById(siteLinkId);
            ChangeObject siteChangeObject = buildSiteLinkChanged(siteLinkId, siteLinkDoc);
            siteLinkChangeObjects.add(siteChangeObject);
        }
        return siteLinkChangeObjects;
    }

    private List<ChangeObject> getRefPhyLinkChangeObjects(String neId) {
        log.debug("get Ref phy link Change objects neId:{}", neId);
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(
                Collections.singletonList(neId));

        List<ChangeObject> phyLinkChangeObjects = new ArrayList<>();
        for (String linkId : phyLinkIds) {
            dciTopologyCacheManager.remove(linkId);
            Document phyLinkDoc = phyLinkDao.getPhyLinkDocumentById(linkId);
            ChangeObject phyChangeObject = buildPhyLinkChangeObject(linkId, phyLinkDoc);
            phyLinkChangeObjects.add(phyChangeObject);
        }
        return phyLinkChangeObjects;
    }


    /**
     * detect and fire node status change for
     *
     * @param neId
     * @param changeBody
     */
    private void detectAndFireNodeStatusChange(String neId, Document changeBody) {
        log.debug("detect and fire node status change,the node:{}", neId);
        Document nePhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (nePhysical == null) {
            log.warn("node:{} status nothing change,discard it", neId);
            return;
        }
        String operationState =
                nePhysical.containsKey(OPERATIONAL_STATE) ? nePhysical.getString(OPERATIONAL_STATE)
                        : null;
        String adminState =
                nePhysical.containsKey(ADMIN_STATE) ? nePhysical.getString(ADMIN_STATE) : null;
        String alignmentState =
                changeBody.containsKey(ALIGNMENT_STATUS) ? changeBody.getString(ALIGNMENT_STATUS)
                        : null;
        NodeStateSnapshot latestState = stateCache.get(neId);
        boolean stateChanged = isStateChanged(latestState, operationState, adminState,
                alignmentState);
        if (!stateChanged) {
            log.debug("skip status event for ne:{} (no state change)", neId);
            return;
        }
        updateCurrentPhyNodeStateCache(neId, latestState, operationState, adminState,
                alignmentState);
        StatusChangeEvent neStatusChangeEvent = StatusChangeEvent.builder().adminStatus(adminState)
                .objectId(neId)
                .statusChangeObjectType(StatusChangeObjectType.DEVICE)
                .operStatus(operationState)
                .alignment(alignmentState)
                .timestamp(System.currentTimeMillis()).build();
//        if (StringUtils.hasText(operationState) || StringUtils.hasText(adminState)) {
        log.info("send phy ne :{} status events notification :{}", neId, neStatusChangeEvent);
        StatusEventNotifier.sendMessage(neStatusChangeEvent);
//        }
    }

    private void updateCurrentPhyNodeStateCache(String neId, NodeStateSnapshot latestState,
            String operationState, String adminState, String alignmentState) {
        NodeStateSnapshot newState =
                latestState != null ? latestState : NodeStateSnapshot.builder().build();

        if (operationState != null) {
            newState.setOperState(operationState);
        }
        if (adminState != null) {
            newState.setAdminState(adminState);
        }
        if (alignmentState != null) {
            newState.setAlignment(alignmentState);
        }
        newState.setTimestamp(System.currentTimeMillis());

        stateCache.put(neId, newState);
    }

    private boolean isStateChanged(NodeStateSnapshot last, String oper,
            String admin, String align) {
        if (oper == null && admin == null && align == null) {
            return false;
        }
        if (last == null) {
            return true;
        }
        boolean changed = false;

        if (oper != null) {
            changed = changed || !Objects.equals(last.getOperState(), oper);
        }
        if (admin != null) {
            changed = changed || !Objects.equals(last.getAdminState(), admin);
        }
        if (align != null) {
            changed = changed || !Objects.equals(last.getAlignment(), align);
        }

        return changed;
    }

    /**
     * monitor the ne implement state changed
     */
    public void onImplementStateChanged(Document source, Document update, String key) {
        log.debug("current implementState changed source document is:{} change key:{}", update,
                key);
        String neId = source.getString(SOURCE_NE_KEY);
        String implementState = "";
        if (key.equals(ON_IMPLEMENT_STATE_DIRECT_PATH)) {
            implementState = update.getString(ON_IMPLEMENT_STATE_DIRECT_PATH);
        } else if (key.equals(ON_IMPLEMENT_STATE_LEVEL_SECONDARY_PATH)) {
            implementState = getImplementStateBySecondaryLevel(key, update);
        } else if (key.equals(ON_IMPLEMENT_STATE_LEVEL_TOP_NUMBER_PATH)) {
            implementState = getImplementStateFromTopLevel(key, update);
        }
        //todo: send implementState change notification for ne
        if (StringUtils.hasText(implementState)) {
            log.debug("send ne implement state change :{}", implementState);
            NodeChangeNotifyBody notifyBody = NodeChangeNotifyBody.builder()
                    .neState(NeStateDto.builder().implementState(implementState).nodeId(neId)
                            .build()).msgType(NE_NOTIFY_TYPE).build();
            NeConnStatusNotifier.sendMessage(notifyBody, neId);
        }
    }


    private String getImplementStateFromTopLevel(String key, Document update) {
        log.debug("get current implement state by key:{}", key);
        Document changeBody = (Document) update.get(key);
        //top number path
        Document physicalDocument = (Document) changeBody.get(PHY_NODE_PHYSICAL);
        return physicalDocument.getString(IMPLEMENT_STATE);
    }

    private String getImplementStateBySecondaryLevel(String key, Document update) {
        log.debug("current implement State key is:{}", key);
        Document physicalDocument = (Document) update.get(key);
        String implementState = physicalDocument.getString(IMPLEMENT_STATE);
        return implementState;
    }
}
