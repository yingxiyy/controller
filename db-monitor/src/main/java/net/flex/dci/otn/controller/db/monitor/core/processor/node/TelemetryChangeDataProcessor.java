package net.flex.dci.otn.controller.db.monitor.core.processor.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.Constants;
import net.flex.dci.otn.controller.db.monitor.utils.PropertiesKeyName;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.TelemetryServerCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/15 16:33
 */
@Component
@Slf4j
public class TelemetryChangeDataProcessor extends AbstractChangeDataProcessor {

    public TelemetryChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }


    public List<ChangeObject> wrapTelemetryForNe(Document sourceDocument, Object update) {
        log.debug("add adapter for the ne");
        List<ChangeObject> changeObjects = new ArrayList<>();
        String telemetryId = sourceDocument.getString(Constants.TELEMETRY_SERVER_ID);
        List<String> updateNeIds = getConnectedNe(update);
        if (!updateNeIds.isEmpty()) {
            changeObjects = getConnectNeChangeObjects(telemetryId, updateNeIds);
        }
        //background method to update the relative cache for the ne
        discardCache(updateNeIds);
        return changeObjects;
    }

    private List<String> getConnectedNe(Object update) {
        List<String> neIds = new ArrayList<>();
        if (update instanceof ArrayList) {
            ((List<Object>) update).forEach(updateDoc -> {
                String neId = ((Document) updateDoc).getString(Constants.NODE_ID);
                neIds.add(neId);
            });
        } else {
            String neId = ((Document) update).getString(Constants.NODE_ID);
            neIds.add(neId);
        }
        return neIds;
    }

    private List<ChangeObject> getConnectNeChangeObjects(String telemetryId,
            List<String> updateNeIds) {
        List<ChangeObject> changeObjects = new ArrayList<>();
        updateNeIds.stream().forEach(neId -> {
            ChangeObject changeObject = new ChangeObject();
            changeObject.setChangeBody(buildAdditionPropertyChangeObjectForNe(
                    PropertiesKeyName.PROPERTY_TELEMETRY_ID, telemetryId, neId));
            changeObject.setObjectType(ObjectType.Node.name());
            changeObject.setEventType(EventType.Update);
            changeObjects.add(changeObject);
            //refresh the telemetry cache
            dciTopologyCacheManager.add(telemetryId, neId);
        });
        return changeObjects;

    }

    public List<ChangeObject> processDelCollector(String deleteCollectorId) {
        log.debug("process the delete collector");
        TelemetryServerCache telemetryServerCache = dciTopologyCacheManager.getValue(
                deleteCollectorId);
        if (null == telemetryServerCache) {
            return new ArrayList<>();
        }
        Set<String> connectedNeIds = telemetryServerCache.getRegisteredNe();
        List<ChangeObject> changeObjects = generateUpdateNesChangeObject(connectedNeIds);
        discardCache(deleteCollectorId);
        discardCache(new ArrayList<>(connectedNeIds));
        return changeObjects;
    }

    private List<ChangeObject> generateUpdateNesChangeObject(Set<String> connectedNeIds) {
        log.debug("generate update nes change object");
        List<ChangeObject> changeObjects = new ArrayList<>();
        connectedNeIds.stream().forEach(neId -> {
            ChangeObject changeObject = new ChangeObject();
            changeObject.setChangeBody(
                    buildAdditionPropertyChangeObjectForNe(PropertiesKeyName.PROPERTY_TELEMETRY_ID,
                            Constants.NONE,
                            neId));
            changeObject.setObjectType(ObjectType.Node.name());
            changeObject.setEventType(EventType.Update);
            changeObjects.add(changeObject);
        });
        return changeObjects;
    }

    
}
