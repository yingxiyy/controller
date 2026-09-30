package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.EQUIPMENT_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ADMIN_STATE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.ALIGNMENT_STATUS;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.StatusEvents.OPERATIONAL_STATE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * 2025/8/26
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class EquipmentChangeDataProcessor extends AbstractChangeDataProcessor {


    private final Map<String, EquipStateSnapshot> stateCache = new ConcurrentHashMap<>();

    private Thread cleanupThread;

    private volatile boolean running = true;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class EquipStateSnapshot {

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

    public EquipmentChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }


    @Override
    public List<ChangeObject> processUpdateObject(ChangeObject changeObject,
            Document sourceDocument) {
        log.debug("background to refresh the termination point cache");
        String eqId = changeObject.getChangeBody().getString(EQUIPMENT_ID);
//        dciTopologyCacheManager.removeAsync(eqId);
        //detect termination point status event change
        detectAndFireEquipmentStatusChange(eqId, changeObject.getChangeBody());
        return Collections.singletonList(changeObject);
    }

    private void detectAndFireEquipmentStatusChange(String eqId, Document changeBody) {
        log.debug("detect current equipment:{} status change", eqId);
        EquipStateSnapshot latest = stateCache.get(eqId);
        String operationState =
                changeBody.containsKey(OPERATIONAL_STATE) ? changeBody.getString(OPERATIONAL_STATE)
                        : null;
        String adminState =
                changeBody.containsKey(ADMIN_STATE) ? changeBody.getString(ADMIN_STATE) : null;
        String alignmentState =
                changeBody.containsKey(ALIGNMENT_STATUS) ? changeBody.getString(ALIGNMENT_STATUS)
                        : null;
        boolean isStateChange = isStateChange(latest, operationState, adminState, alignmentState);
        if (!isStateChange) {
            log.debug("skip status event for equipment:{} (no state change)", eqId);
            return;
        }
        updateStateCache(eqId, latest, operationState, adminState, alignmentState);
        StatusChangeEvent tpStatusChangeEvent = StatusChangeEvent.builder().adminStatus(adminState)
                .objectId(eqId)
                .statusChangeObjectType(StatusChangeObjectType.CARD)
                .alignment(alignmentState)
                .operStatus(operationState).build();
//        if (StringUtils.hasText(operationState) || StringUtils.hasText(adminState)) {
        log.info("send equipment status events notification :{}", tpStatusChangeEvent);
        StatusEventNotifier.sendMessage(tpStatusChangeEvent);

//        }
    }

    private void updateStateCache(String eqId, EquipStateSnapshot latest, String operationState,
            String adminState, String alignmentState) {
        EquipStateSnapshot equipStateSnapshot =
                latest != null ? latest : EquipStateSnapshot.builder()
                        .build();
        if (operationState != null) {
            equipStateSnapshot.setOperState(operationState);
        }
        if (adminState != null) {
            equipStateSnapshot.setAdminState(adminState);
        }
        if (alignmentState != null) {
            equipStateSnapshot.setAlignment(alignmentState);
        }
        equipStateSnapshot.setTimestamp(System.currentTimeMillis());
        stateCache.put(eqId, equipStateSnapshot);
    }

    private boolean isStateChange(EquipStateSnapshot latest, String operationState,
            String adminState, String alignmentState) {
        if (operationState == null && adminState == null && alignmentState == null) {
            return false;
        }
        if (latest == null) {
            return true;
        }
        boolean changed = false;
        if (operationState != null) {
            changed = changed || !Objects.equals(latest.getOperState(), operationState);
        }
        if (adminState != null) {
            changed = changed || !Objects.equals(latest.getAdminState(), adminState);
        }
        if (alignmentState != null) {
            changed = changed || !Objects.equals(latest.getAlignment(), alignmentState);
        }
        return changed;
    }
}
