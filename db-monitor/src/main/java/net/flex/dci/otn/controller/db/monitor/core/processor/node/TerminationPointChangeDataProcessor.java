package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.MODEL_SPEC_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.OTU_LINE_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PORT_IN_ID_SUFFIX;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PORT_OUT_ID_SUFFIX;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.REG_PORT_PREFIX;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.SERVICE_TYPE_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ADMIN_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ALIGNMENT_STATUS;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.OPERATIONAL_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.PHY_PHYSICAL_KEY;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TERMINATION_POINT_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TP_ATTRIBUTE_CONTAINER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.StatusChangeObjectType;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.tools.kafka.service.StatusEventNotifier;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.bson.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2025/8/26
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class TerminationPointChangeDataProcessor extends AbstractChangeDataProcessor {

    public TerminationPointChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }

    private final Map<String, TpStateSnapshot> stateCache = new ConcurrentHashMap<>();

    private Thread cleanupThread;

    private volatile boolean running = true;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TpStateSnapshot {

        String operState;
        String adminState;
        String alignment;
        long timestamp;
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
        }, "phy-node-equip-cache-cleaner");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        log.info("PhyNode equip state cache cleanup thread started");
    }

    @PreDestroy
    public void stopCleanupThread() {
        running = false;
        if (cleanupThread != null) {
            cleanupThread.interrupt();
        }
        log.info("PhyNode equip state cache cleanup thread stopped");
    }

    private void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        long ttlMillis = 5 * 60 * 1000;

        stateCache.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().getTimestamp();
            return age > ttlMillis;
        });
    }

    @Override
    public List<ChangeObject> processUpdateObject(ChangeObject changeObject,
            Document sourceDocument) {
        log.debug("background to refresh the termination point cache");
        List<ChangeObject> changeObjects = new ArrayList<>();
        changeObjects.add(changeObject);

        String tpId = changeObject.getChangeBody().getString(TERMINATION_POINT_ID);
        Document tpDoc = currentTpDoc(tpId, sourceDocument);
        boolean isReg = isRegTp(tpDoc);
        if (isReg) {
            List<ChangeObject> regChangeObjects = buildRegChangeObject(changeObject);
            changeObjects.addAll(regChangeObjects);
        }
//        dciTopologyCacheManager.removeAsync(tpId);
        //detect termination point status event change
        detectAndFireTpStatusChange(tpId, changeObject.getChangeBody());
        return changeObjects;
    }


    private List<ChangeObject> buildRegChangeObject(ChangeObject originalChangeObject) {
        List<ChangeObject> regChangeObjects = new ArrayList<>(2);

        if (originalChangeObject == null) {
            return regChangeObjects;
        }

        Document originalBody = originalChangeObject.getChangeBody();
        if (originalBody == null) {
            return regChangeObjects;
        }
        String originalTpId = originalBody.getString(TERMINATION_POINT_ID);
        if (!StringUtils.hasText(originalTpId)) {
            return regChangeObjects;
        }

        String inTpId = originalTpId + PORT_IN_ID_SUFFIX;
        String outTpId = originalTpId + PORT_OUT_ID_SUFFIX;

        ChangeObject inChangeObject = copyAndModifyTpId(originalChangeObject, inTpId);
        regChangeObjects.add(inChangeObject);

        ChangeObject outChangeObject = copyAndModifyTpId(originalChangeObject, outTpId);
        regChangeObjects.add(outChangeObject);

        log.debug("generate reg port object two direction: origin={}, IN={}, OUT={}", originalTpId,
                inTpId, outTpId);
        return regChangeObjects;
    }

    private ChangeObject copyAndModifyTpId(ChangeObject origin, String tpId) {
        Document newBody = Document.parse(origin.getChangeBody().toJson());
        newBody.put(TERMINATION_POINT_ID, tpId);
        ChangeObject changeObject = new ChangeObject();
        changeObject.setObjectType(origin.getObjectType());
        changeObject.setChangeBody(newBody);
        changeObject.setEventType(origin.getEventType());
        changeObject.setCollectionName(origin.getCollectionName());
        changeObject.setNeId(origin.getNeId());
        changeObject.setObjectType(origin.getObjectType());
        return changeObject;
    }

    private Document currentTpDoc(String tpId, Document sourceDocument) {
        if (sourceDocument == null || !StringUtils.hasText(tpId)) {
            log.warn("currentTpDoc parameter exception, tpId:{}, sourceDocument is null", tpId);
            return null;
        }
        try {
            Document data = sourceDocument.get("data", Document.class);
            if (data == null) {
                log.warn("Data node not found in sourceDocument");
                return null;
            }
            List<Document> nodeList = data.getList("node", Document.class);
            if (nodeList == null || nodeList.isEmpty()) {
                log.warn("Node list not found in data node");
                return null;
            }
            Document node = nodeList.get(0);

            List<Document> terminationPoints = node.getList("termination-point", Document.class);
            if (terminationPoints == null || terminationPoints.isEmpty()) {
                log.warn("Termination-point list not found in node");
                return null;
            }
            Map<String, Document> terminationPoinMap = terminationPoints.stream().collect(
                    Collectors.toMap(tp -> tp.getString(TERMINATION_POINT_ID), tp -> tp,
                            (oldValue, newValue) -> oldValue));
            Document refTp = terminationPoinMap.get(tpId);
            if (refTp == null) {
                log.debug("Termination point not found, tpId:{}", tpId);
            }
            return refTp;
        } catch (Exception e) {
            log.error("Failed to parse termination point document, tpId:{}", tpId, e);
            return null;
        }
    }

    private boolean isRegTp(Document tpDoc) {
        if (tpDoc == null) {
            return false;
        }

        try {

            Document tpPhysical = tpDoc.get(TP_ATTRIBUTE_CONTAINER, Document.class);
            if (tpPhysical == null) {
                return false;
            }

            Document otuLineDoc = tpPhysical.get(OTU_LINE_KEY, Document.class);
            if (otuLineDoc == null) {
                return false;
            }

            Document modelSpecDoc = otuLineDoc.get(MODEL_SPEC_KEY, Document.class);
            if (modelSpecDoc == null) {
                return false;
            }

            String serviceType = modelSpecDoc.getString(SERVICE_TYPE_KEY);
            return serviceType != null && serviceType.startsWith(REG_PORT_PREFIX);
        } catch (Exception e) {
            log.warn("check REG port failed, tpDoc: {}", tpDoc, e);
            return false;
        }
    }

    /**
     * @param tpId
     * @param changeBody
     */
    private void detectAndFireTpStatusChange(String tpId, Document changeBody) {
        log.debug("detect current tp:{} status change", tpId);

        Document tpPhysical = Arrays.stream(PHY_PHYSICAL_KEY)
                .filter(changeBody::containsKey)
                .findFirst()
                .map(key -> (Document) changeBody.get(key))
                .orElse(null);
        if (tpPhysical == null) {
            log.trace("no status event change,do nothing and discard it");
            return;
        }
        String operationState =
                tpPhysical.containsKey(OPERATIONAL_STATE) ? tpPhysical.getString(OPERATIONAL_STATE)
                        : null;
        String adminState =
                tpPhysical.containsKey(ADMIN_STATE) ? tpPhysical.getString(ADMIN_STATE) : null;
        String alignmentState =
                changeBody.containsKey(ALIGNMENT_STATUS) ? changeBody.getString(ALIGNMENT_STATUS)
                        : null;
        TpStateSnapshot latest = stateCache.get(tpId);
        boolean isChanged = isStateChanged(latest, operationState, adminState, alignmentState);
        if (!isChanged) {
            log.debug("skip tp state change for tp:{} (no state change)", tpId);
            return;
        }
        updateTpStateCache(tpId, latest, operationState, adminState, alignmentState);
        StatusChangeEvent tpStatusChangeEvent = StatusChangeEvent.builder().adminStatus(adminState)
                .objectId(tpId)
                .statusChangeObjectType(StatusChangeObjectType.TERMINATION_POINT)
                .operStatus(operationState)
                .alignment(alignmentState)
                .timestamp(System.currentTimeMillis()).build();
//        if (StringUtils.hasText(operationState) || StringUtils.hasText(adminState)) {
        log.info("send termination point status events notification :{}", tpStatusChangeEvent);
        StatusEventNotifier.sendMessage(tpStatusChangeEvent);

//        }
    }

    private void updateTpStateCache(String tpId, TpStateSnapshot latest, String operationState,
            String adminState, String alignmentState) {
        TpStateSnapshot tpStateSnapshot =
                latest != null ? latest : TpStateSnapshot.builder().build();
        if (operationState != null) {
            tpStateSnapshot.setOperState(operationState);
        }
        if (adminState != null) {
            tpStateSnapshot.setAdminState(adminState);
        }
        if (alignmentState != null) {
            tpStateSnapshot.setAlignment(alignmentState);
        }
        tpStateSnapshot.setTimestamp(System.currentTimeMillis());
        stateCache.put(tpId, tpStateSnapshot);
    }

    private boolean isStateChanged(TpStateSnapshot latest, String operationState, String adminState,
            String alignmentState) {
        if (operationState == null && adminState == null && alignmentState == null) {
            return false;
        }
        if (latest == null) {
            return true;
        }
        boolean changed = false;
        if (operationState != null) {
            changed = changed || !Objects.equals(operationState, latest.getOperState());
        }
        if (adminState != null) {
            changed = changed || !Objects.equals(adminState, latest.getAdminState());
        }
        if (alignmentState != null) {
            changed = changed || !Objects.equals(alignmentState, latest.getAlignment());
        }
        return changed;
    }
}
