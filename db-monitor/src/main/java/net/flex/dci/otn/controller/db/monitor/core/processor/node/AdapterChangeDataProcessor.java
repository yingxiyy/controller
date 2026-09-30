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
import net.flex.dci.otn.topology.cache.model.AdapterCache;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/14 15:56
 */
@Component
@Slf4j
public class AdapterChangeDataProcessor extends AbstractChangeDataProcessor {


    public AdapterChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }

    /**
     * @param sourceDocument
     * @param updateDocument
     * @return
     */
    public List<ChangeObject> wrapAdapterForNe(Document sourceDocument, Object updateDocument) {
        log.debug("add adapter for the ne");
        List<ChangeObject> changeObjects = new ArrayList<>();
        String adapterId = sourceDocument.getString(Constants.ADAPTER_ID);
        List<String> updateNeIds = getConnectedNe(updateDocument);
        if (!updateNeIds.isEmpty()) {
            changeObjects = getConnectNeChangeObjects(adapterId, updateNeIds);
        }
        //background method to update the relative cache for the ne
        discardCache(updateNeIds);
        return changeObjects;
    }

    private List<ChangeObject> getConnectNeChangeObjects(String adapterId,
            List<String> updateNeIds) {
        List<ChangeObject> changeObjects = new ArrayList<>();
        updateNeIds.stream().forEach(neId -> {
            ChangeObject changeObject = new ChangeObject();
            changeObject.setChangeBody(
                    buildAdditionPropertyChangeObjectForNe(PropertiesKeyName.PROPERTY_ADAPTER_ID,
                            adapterId,
                            neId));
            changeObject.setObjectType(ObjectType.Node.name());
            changeObjects.add(changeObject);
        });
        return changeObjects;
    }

    private List<String> getConnectedNe(Object updateDocument) {
        List<String> neIds = new ArrayList<>();
        if (updateDocument instanceof ArrayList) {
            ((List<Object>) updateDocument).forEach(update -> {
                String neId = ((Document) update).getString(Constants.NODE_ID);
                neIds.add(neId);
            });
        } else {
            String neId = ((Document) updateDocument).getString(Constants.NODE_ID);
            neIds.add(neId);
        }
        return neIds;
    }


    public List<ChangeObject> processDelAdapter(String deleteAdapterId) {
        log.debug("process the delete adapter");
        AdapterCache adapterCache = dciTopologyCacheManager.getValue(deleteAdapterId);
        if (null == adapterCache) {
            return new ArrayList<>();
        }
        Set<String> connectedNeIds = adapterCache.getRegisteredNe();
        List<ChangeObject> changeObjects = generateUpdateNesChangeObject(connectedNeIds);
        discardCache(deleteAdapterId);
        discardCache(new ArrayList<>(connectedNeIds));
        return changeObjects;
    }

    private List<ChangeObject> generateUpdateNesChangeObject(Set<String> connectedNeIds) {
        log.debug("generate update nes change object");
        List<ChangeObject> changeObjects = new ArrayList<>();
        connectedNeIds.stream().forEach(neId -> {
            ChangeObject changeObject = new ChangeObject();
            changeObject.setChangeBody(
                    buildAdditionPropertyChangeObjectForNe(PropertiesKeyName.PROPERTY_ADAPTER_ID,
                            Constants.NONE,
                            neId));
            changeObject.setObjectType(ObjectType.Node.name());
            changeObject.setEventType(EventType.Update);
            changeObjects.add(changeObject);
        });
        return changeObjects;
    }


}
