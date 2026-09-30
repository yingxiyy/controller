package net.flex.dci.otn.controller.db.monitor.core;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.CONFIG_COLLECTION_PREFIX;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OP_COLLECTION_PREFIX;
import static net.flex.dci.otn.controller.db.monitor.utils.ChangeObjectUtils.buildNetConfEventChangeDto;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DATA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.IChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.DefaultBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.mapper.CollectionRefNetConfMapper;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.CollectionDto;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.bson.Document;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2021/11/4 13:44
 */
@Slf4j
public class NetConfEventChangeHandlerDispatcher {


    public static final Map<String, IChangeBodyHandler> changeBodyHandlerMap = new HashMap<>();
    private final DciTopologyCacheManager dciTopologyCacheManager;

    public NetConfEventChangeHandlerDispatcher(DciTopologyCacheManager dciTopologyCacheManager,
            List<AbstractBodyChangeHandler> handlers) {
        this.dciTopologyCacheManager = dciTopologyCacheManager;
        handlers.forEach(changeHandler -> {
            changeBodyHandlerMap.put(changeHandler.getCollection(), changeHandler);
        });
    }

    /**
     * extract info from collection name
     *
     * @param collectionName
     * @param document
     * @return
     */
    public List<NetConfEventChangeDto> extractCreateInfo(String collectionName,
            Document document) {
        log.debug("start to extract info from the collection :{} create info", collectionName);
        DataStoreType dataStoreType = extractDataStoreType(collectionName);
        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);
//        String dataKeyValue = (String) document.get(collectionDto.getKeyName());
        String dataKey = collectionDto.getDataKey();
        Document createDocument = getCreateDocument(dataKey, document);
        List<ChangeObject> changeObjects = extractCreateObject(collectionName,
                createDocument);
        if (CollectionUtils.isEmpty(changeObjects)) {
            return null;
        }
        List<NetConfEventChangeDto> netConfEventChangeDtos = changeObjects.stream()
                .map(changeObject -> buildNetConfEventChangeDto(changeObject, collectionDto,
                        dataStoreType)).collect(
                        Collectors.toList());
        return netConfEventChangeDtos;
//        netConfEventChangeDto.setNewData((Document) document.get("data"));

    }

    private Document getCreateDocument(String dataKey, Document document) {
        Document createDocument = null;
        Object createObject = ((Document) document.get(DATA)).get(dataKey);
        if (createObject instanceof Document) {
            createDocument = (Document) createObject;
        } else if (createObject instanceof ArrayList) {
            List<Object> details = (List<Object>) createObject;
            createDocument = (Document) details.get(0);
        }
        return createDocument;
    }

    /**
     * extract delete detail info
     *
     * @param collectionName
     * @param deleteDoc
     * @return
     */
    public List<NetConfEventChangeDto> extractDeleteInfo(String collectionName, Document sourceDoc,
            Document deleteDoc) {
        if (null == deleteDoc) {
            return new ArrayList<>();
        }
        DataStoreType dataStoreType = extractDataStoreType(collectionName);
        log.debug("start to extract info for the collection:{} delete", collectionName);
        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);
        List<ChangeObject> changeObjects = extractDeleteChangeObject(collectionName, sourceDoc,
                deleteDoc);
        List<NetConfEventChangeDto> netConfEventChangeDtos = changeObjects.stream()
                .map(changeObject -> buildNetConfEventChangeDto(changeObject, collectionDto,
                        dataStoreType)).collect(
                        Collectors.toList());
        return netConfEventChangeDtos;
//        NetConfEventChangeDto netConfEventChangeDto = new NetConfEventChangeDto();
//        log.debug("start to extract info from the collection :{} delete info", collectionName);
//        DataStoreType dataStoreType = extractDataStoreType(collectionName);
//        if (dataStoreType.equals(DataStoreType.OPERATIONAL)) {
//            return new ArrayList<>();
//        }
//        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);
//        String deleteKeyId = deleteDoc.getString(DELETE_OP);
//        dciTopologyCacheManager.removeAsync(deleteKeyId);
//        Document changeData = new Document(collectionDto.getObjectKeyName(), deleteKeyId);
//        netConfEventChangeDto.setObjectType(collectionDto.getObjectType());
//        netConfEventChangeDto.setTopologyRef(collectionDto.getTopologyRef());
//        netConfEventChangeDto.setTopologyType(collectionDto.getTopologyType());
//        netConfEventChangeDto.setChangeData(changeData);
//        netConfEventChangeDto.setObjectId(deleteKeyId);
//        netConfEventChangeDto.setDataStoreType(dataStoreType);
//        return Collections.singletonList(netConfEventChangeDto);
    }

    private List<ChangeObject> extractDeleteChangeObject(String collectionName, Document sourceDoc,
            Document deleteDoc) {
        log.debug("start to delete change object for the change object,collection name is {}",
                collectionName);
        DataStoreType dataStoreType = extractDataStoreType(collectionName);
        List<ChangeObject> changeObjects = changeBodyHandlerMap.getOrDefault(
                        refactorCollectionName(collectionName), new DefaultBodyChangeHandler())
                .extractDeleteChangeObject(dataStoreType, collectionName, sourceDoc, deleteDoc);
        return changeObjects;
    }

    public List<NetConfEventChangeDto> extractUpdateInfo(String collectionName,
            Document sourceDoc, Document updateDoc) {
        if (sourceDoc == null || updateDoc == null) {
            return new ArrayList<>();
        }
        log.debug("start to extract info from the collection :{} update info", collectionName);
        DataStoreType dataStoreType = extractDataStoreType(collectionName);

        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);

        List<ChangeObject> changeObjects = extractChangeObject(
                collectionName,
                sourceDoc, updateDoc);
        if (changeObjects == null || changeObjects.isEmpty()) {
            return new ArrayList<>();
        }
        List<NetConfEventChangeDto> netConfEventChangeDtos = changeObjects.stream()
                .map(changeObject -> buildNetConfEventChangeDto(changeObject, collectionDto,
                        dataStoreType, sourceDoc)).collect(
                        Collectors.toList());

//
//        netConfEventChangeDto.setDataStoreType(dataStoreType);
        return netConfEventChangeDtos;
    }


    private Document buildUpdateDocument(String path, Object updateObject) {
        Document updateDocument = new Document();
        Object temp = updateObject;
        if (path.contains("0.")) {
            String subPath = path.substring(path.indexOf("0") + 2);
            String[] subPaths = subPath.split("\\.");
            for (int i = subPaths.length - 1; i >= 0; i--) {
                String subKey = subPaths[i];
                updateDocument = new Document().append(subKey, temp);
                temp = updateDocument;
            }
        } else {
            if (updateObject instanceof ArrayList) {
                updateDocument = (Document) ((ArrayList) updateObject).get(0);
            } else {
                updateDocument = (Document) updateObject;
            }

        }
        return updateDocument;
    }


    private DataStoreType extractDataStoreType(String collectionName) {
        if (collectionName.startsWith("op-")) {
            return DataStoreType.OPERATIONAL;
        }
        return DataStoreType.CONFIG;
    }

    /**
     * handle remove attribute info
     *
     * @param collectionName
     * @param sourceDoc
     * @param unsetDoc
     * @return
     */
    public List<NetConfEventChangeDto> extractRemoveAttributeInfo(String collectionName,
            Document sourceDoc, Document unsetDoc) {
        log.info("extract remove attribute info updateDoc is :{}", unsetDoc);

        log.debug("start to extract info from the collection :{} update info", collectionName);
        DataStoreType dataStoreType = extractDataStoreType(collectionName);
        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);

        ChangeObject changeObject = extractUnsetChangeObject(
                collectionName,
                sourceDoc, unsetDoc);
        if (changeObject == null) {
            return new ArrayList<>();
        }
        NetConfEventChangeDto netConfEventChangeDto = buildNetConfEventChangeDto(changeObject,
                collectionDto, dataStoreType, sourceDoc);
        return Collections.singletonList(netConfEventChangeDto);
    }


    public List<ChangeObject> extractChangeObject(String collectionName, Document sourceDoc,
            Document updateDoc) {
        log.debug("start to update change object for the change object,collection name is {}",
                collectionName);
        List<ChangeObject> changeObjects = changeBodyHandlerMap.getOrDefault(
                        refactorCollectionName(collectionName), new DefaultBodyChangeHandler())
                .extractSetChangeObject(sourceDoc, updateDoc);
        return changeObjects;
    }

    public ChangeObject extractUnsetChangeObject(String collectionName, Document sourceDoc,
            Document unsetDoc) {
        log.debug("start to unset change object for the change object,collection name is {}",
                collectionName);
        ChangeObject changeObject = changeBodyHandlerMap.getOrDefault(
                        refactorCollectionName(collectionName), new DefaultBodyChangeHandler())
                .extractUNSETChangeObject(sourceDoc, unsetDoc);
        return changeObject;
    }

    private String refactorCollectionName(String collectionName) {
        return collectionName.replace(OP_COLLECTION_PREFIX, "")
                .replace(CONFIG_COLLECTION_PREFIX, "")
                .replace("-col", "");
    }

    public List<ChangeObject> extractCreateObject(String collectionName, Document document) {
        log.debug("start to update change object for the change object,collection name is {}",
                collectionName);
        List<ChangeObject> changeObjects = changeBodyHandlerMap.getOrDefault(
                        refactorCollectionName(collectionName), new DefaultBodyChangeHandler())
                .extractCreateObject(refactorCollectionName(collectionName), document);
        return changeObjects;
    }


}
