package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import static net.flex.dci.otn.controller.db.monitor.utils.ChangeObjectUtils.buildNetConfEventChangeDto;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.FRIENDLY_NAME;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.PHY_PHYSICAL_KEY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.NotificationService;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.springframework.stereotype.Component;

/**
 * 2026/5/9
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class PhyNodeNameChangeQueueProcessor extends AbstractChangeDataProcessor {

    private static final int CONSUMER_THREAD = 5;

    private final NotificationService notificationService;


    public final TunnelDao tunnelDao;

    public final SiteLinkDao siteLinkDao;

    public final PhyLinkDao phyLinkDao;


    private final BlockingQueue<NameChangeTask> nameChangeTaskBlockingQueue = new LinkedBlockingQueue<>();
    private final List<Thread> consumerThreads = new ArrayList<>();
    private volatile boolean running = true;

    public PhyNodeNameChangeQueueProcessor(DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory,
            NotificationService notificationService, TunnelDao tunnelDao,
            SiteLinkDao siteLinkDao, PhyLinkDao phyLinkDao) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
        this.notificationService = notificationService;
        this.tunnelDao = tunnelDao;
        this.siteLinkDao = siteLinkDao;
        this.phyLinkDao = phyLinkDao;

    }

    @PostConstruct
    public void startExecutor() {
        for (int i = 0; i < CONSUMER_THREAD; i++) {
            Thread consumer = new Thread(this::consumerTask, "phy-node-name-consumer-" + i);
            consumer.setDaemon(true);
            consumer.start();
            consumerThreads.add(consumer);
        }
        log.info("Phy node name change queue processor started with :{} consumer", CONSUMER_THREAD);
    }

    @PreDestroy
    public void stopConsumers() {
        running = false;
        consumerThreads.forEach(Thread::interrupt);
        log.info("PhyNode name change queue processor stopped");
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class NameChangeTask implements Serializable {

        private String neId;

        private Document changeBody;

        private String newFriendlyName;

        private long submitTime;
    }

    public void submitNodeNameChange(String neId, Document changeBody) {
        log.debug("detect and fire node:{} friendlyName change", neId);
        Document nePhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (nePhysical == null) {
            log.debug("node:{} physical attribute nothing change,discard it", neId);
            return;
        }
        String friendlyName =
                nePhysical.containsKey(FRIENDLY_NAME) ? nePhysical.getString(FRIENDLY_NAME) : null;
        if (friendlyName == null) {
            log.debug("neName do not change discard it,skip it");
            return;
        }
        NameChangeTask nameChangeTask = NameChangeTask.builder().neId(neId).changeBody(changeBody)
                .newFriendlyName(friendlyName)
                .submitTime(System.currentTimeMillis()).build();
        try {
            nameChangeTaskBlockingQueue.put(nameChangeTask);
            log.debug("Queued name change for ne:{}, queueSize:{}", neId,
                    nameChangeTaskBlockingQueue.size());
            return;
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while submitting name change for ne:{}", neId);
            return;
        }

    }

    public void consumerTask() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                NameChangeTask task = nameChangeTaskBlockingQueue.poll(100,
                        TimeUnit.MILLISECONDS);
                if (task != null) {
                    long waitTIme = System.currentTimeMillis() - task.getSubmitTime();
                    log.info("execute the task for neId:{} wait:{} ms,queueSize:{}", task.neId,
                            waitTIme, nameChangeTaskBlockingQueue.size());
                    processNameChange(task.getNeId(), task.getChangeBody(),
                            task.getNewFriendlyName());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to process name change task", e);
            }
        }
    }

    private void processNameChange(String neId, Document changeBody, String friendlyName) {
        try {
            //update the name cache
            updateTheNeCache(neId, friendlyName);
//            dciTopologyCacheManager.removeAsync(neId);
            List<ChangeObject> refPhyLinkChangeObjects = getRefPhyLinkChangeObjects(neId,
                    friendlyName);
            List<ChangeObject> refSiteLinkChangeObjects = getRefSiteLinkChangeObjects(neId,
                    friendlyName);
            List<ChangeObject> refTunnelChangeObjects = getRefTunnelChangeObjects(neId,
                    friendlyName);
            List<ChangeObject> totalChangeObjects = Stream.of(refTunnelChangeObjects,
                            refSiteLinkChangeObjects,
                            refPhyLinkChangeObjects)
                    .flatMap(List::stream).collect(
                            Collectors.toList());
            sendNotification(totalChangeObjects);
        } catch (Exception ex) {
            log.error("Failed to process name change ", ex.getCause());
            log.error("Failed to process name change for ne:{} :{}", neId, ex.getMessage(), ex);
        }
    }

    private void updateTheNeCache(String neId, String friendlyName) {
        log.debug("update the ne cache,the neId:{} new friendlyName:{}", neId, friendlyName);
        PhyNodeCache nodeCache = dciTopologyCacheManager.getValue(neId, PhyNodeCache.class);
        if (nodeCache != null) {
            nodeCache.setFriendlyName(friendlyName);
            dciTopologyCacheManager.updateValue(neId, nodeCache);
        }
    }

    private void sendNotification(List<ChangeObject> totalChangeObjects) {
        log.debug("start to send notification for the total change objects size:{}",
                totalChangeObjects.size());
        List<NetConfEventChangeDto> netConfEventChangeDtos = totalChangeObjects.stream()
                .map(changeObject -> buildNetConfEventChangeDto(changeObject, null,
                        DataStoreType.CONFIG, changeObject.getChangeBody())).collect(
                        Collectors.toList());
        netConfEventChangeDtos.forEach(
                netConfEventChangeDto -> notificationService.publishNotification(
                        EventType.Update, netConfEventChangeDto));
    }

    private List<ChangeObject> getRefTunnelChangeObjects(String neId, String friendlyName) {
        log.debug("get Ref tunnel change object neId:{}", neId);
        List<LinkStateDto> tunnels = tunnelDao.queryTunnelsStateWithNode(neId);
        if (tunnels.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tunnelIds = tunnels.stream().map(LinkStateDto::getId).collect(
                Collectors.toList());
        List<Document> tunnelDocuments = tunnelDao.getTunnelDocumentsByIds(tunnelIds);
        List<ChangeObject> tunnelChangeObjects = new ArrayList<>();

        for (Document tunnelDoc : tunnelDocuments) {
            String tunnelId = ((Document) tunnelDoc.get("tunnel")).getString("tunnel-id");
            log.info("update tunnel :{} ref cache ", tunnelId);
            updateTunnelCache(tunnelId, neId, friendlyName);
//            dciTopologyCacheManager.remove(tunnelId, TunnelCache.class);
            ChangeObject siteChangeObject = buildTunnelChanged(tunnelId, tunnelDoc);
            tunnelChangeObjects.add(siteChangeObject);
        }
        return tunnelChangeObjects;
    }


    private List<ChangeObject> getRefSiteLinkChangeObjects(String neId, String friendlyName) {
        log.debug("get Ref site link change object neId:{} ", neId);
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsByPhyNodeId(
                Collections.singletonList(neId));
        if (siteLinkIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> siteLinkDocuments = siteLinkDao.getSiteLinkDocumentsByIds(siteLinkIds);
        List<ChangeObject> siteLinkChangeObjects = new ArrayList<>();
        for (Document siteLinkDocument : siteLinkDocuments) {
            String linkId = ((Document) siteLinkDocument.get("link")).getString("link-id");
            log.info("update site link :{} ref cache ", linkId);
            updateSiteLinkCache(linkId, neId, friendlyName);
//            dciTopologyCacheManager.remove(linkId, SiteLinkCache.class);
            ChangeObject siteChangeObject = buildSiteLinkChanged(linkId, siteLinkDocument);
            siteLinkChangeObjects.add(siteChangeObject);
        }
        return siteLinkChangeObjects;
    }


    private List<ChangeObject> getRefPhyLinkChangeObjects(String neId, String friendlyName) {
        log.debug("get Ref phy link Change objects neId:{}", neId);
        List<String> phyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(
                Collections.singletonList(neId));
        if (phyLinkIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Document> phyLinkDocuments = phyLinkDao.getPhyLinkDocumentsByIds(phyLinkIds);
        List<ChangeObject> phyLinkChangeObjects = new ArrayList<>();
        for (Document phyLinkDoc : phyLinkDocuments) {
            String phyLinkId = ((Document) phyLinkDoc.get("link")).getString("link-id");
            log.info("update phy link :{} ref cache ", phyLinkId);
//
//            dciTopologyCacheManager.remove(phyLinkId, PhyLinkCache.class);
            updatePhyLinkCache(phyLinkId, neId, friendlyName);
            ChangeObject phyChangeObject = buildPhyLinkChangeObject(phyLinkId, phyLinkDoc);
            phyLinkChangeObjects.add(phyChangeObject);
        }
        return phyLinkChangeObjects;
    }

    private void updatePhyLinkCache(String phyLinkId, String neId, String friendlyName) {
        log.debug("update the phy link cache the phy link id:{} the neId:{} and friendly name:{}",
                phyLinkId, neId, friendlyName);
        PhyLinkCache phyLinkCache = dciTopologyCacheManager.getValue(phyLinkId, PhyLinkCache.class);
        if (phyLinkCache != null) {
            if (phyLinkCache.getSource().getNodeId().equals(neId)) {
                phyLinkCache.getSource().setNodeName(friendlyName);
            }
            if (phyLinkCache.getDestination().getNodeId().equals(neId)) {
                phyLinkCache.getDestination().setNodeName(friendlyName);
            }
            dciTopologyCacheManager.updateValue(phyLinkId, phyLinkCache);
        }
    }


    private void updateSiteLinkCache(String linkId, String neId, String friendlyName) {
        log.debug("update the site link cache the phy link id:{} the neId:{} and friendly name:{}",
                linkId, neId, friendlyName);
        SiteLinkCache siteLinkCache = dciTopologyCacheManager.getValue(linkId, SiteLinkCache.class);
        if (siteLinkCache != null) {
            if (siteLinkCache.getSource().getNodeId().equals(neId)) {
                siteLinkCache.getSource().setNodeName(friendlyName);
            }
            if (siteLinkCache.getDestination().getNodeId().equals(neId)) {
                siteLinkCache.getDestination().setNodeName(friendlyName);
            }
            dciTopologyCacheManager.updateValue(linkId, siteLinkCache);
        }
    }

    private void updateTunnelCache(String tunnelId, String neId, String friendlyName) {
        log.debug("update the tunnel cache the phy link id:{} the neId:{} and friendly name:{}",
                tunnelId, neId, friendlyName);
        TunnelCache tunnelCache = dciTopologyCacheManager.getValue(tunnelId, TunnelCache.class);
        if (tunnelCache != null) {
            if (tunnelCache.getSource().getNodeId().equals(neId)) {
                tunnelCache.getSource().setNodeName(friendlyName);
            }
            if (tunnelCache.getDestination().getNodeId().equals(neId)) {
                tunnelCache.getDestination().setNodeName(friendlyName);
            }
            dciTopologyCacheManager.updateValue(tunnelId, tunnelCache);
        }
    }


}
