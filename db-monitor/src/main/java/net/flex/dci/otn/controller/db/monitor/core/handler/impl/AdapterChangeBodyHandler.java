package net.flex.dci.otn.controller.db.monitor.core.handler.impl;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.ADAPTER_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.COLON;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DELETE_OP;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.mapper.GeneralCollectionRefObjectTypeMapper;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/10/25 16:25
 */
@Slf4j
@Component
public class AdapterChangeBodyHandler extends AbstractBodyChangeHandler {


    @Override
    public String getCollection() {
        return ADAPTER_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        List<ChangeObject> changeObjects = new ArrayList<>();
        for (Entry<String, Object> entry : updateDocument.entrySet()) {
            String key = entry.getKey();
            List<ChangeObject> changeObjectList = extractSetChangeObject(key, sourceDocument,
                    entry.getValue());
            if (!changeObjectList.isEmpty()) {
                changeObjects.addAll(changeObjectList);
            }

        }

        return changeObjects;
    }

    @Override
    public List<ChangeObject> extractCreateObject(String collectionName, Document createDoc) {
        log.info("start to extract create body :{}", createDoc.toJson());
        ChangeObject changeObject = null;
        Document changeDoc = new Document();
        for (Entry<String, Object> entry : createDoc.entrySet()) {
            String key = entry.getKey();
            if (key.contains(ID)) {
                changeDoc.put(key, entry.getValue());
            } else if (key.contains(COLON)) {
                changeDoc.put(key.split(COLON)[1], entry.getValue());
            }
        }
        if (!changeDoc.isEmpty()) {
            changeObject = new ChangeObject();
            changeObject.setChangeBody(changeDoc);
            changeObject.setObjectType(
                    GeneralCollectionRefObjectTypeMapper.getGeneralObjectType(collectionName));
            return Collections.singletonList(changeObject);
        }
        return null;
    }

    @Override
    public List<ChangeObject> extractDeleteChangeObject(DataStoreType dataStoreType,
            String collectionName, Document sourceDocument, Document deleteDoc) {
        log.debug("set for delete Change object for adapter");
        String deleteAdapterId = deleteDoc.getString(DELETE_OP);
        List<ChangeObject> changeObjects = adapterChangeDataProcessor.processDelAdapter(
                deleteAdapterId);
//        List<String> connectedNeIds = getConnectedNeIds(sourceDocument);
//        List<ChangeObject> changeObjects = adapterChangeDataWrapper.removeAdapterInfoForNe(
//                connectedNeIds);
        return changeObjects;
    }

    private List<ChangeObject> extractSetChangeObject(String key, Document sourceDocument,
            Object update) {
        List<ChangeObject> changeObjects = new ArrayList<>();
        if (key.equals("data.adapter.0.ne") || key.contains("data.adapter.0.ne.")) {
            changeObjects = adapterChangeDataProcessor.wrapAdapterForNe(sourceDocument,
                    update);
        }
        return changeObjects;
    }


}
