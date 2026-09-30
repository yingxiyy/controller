/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.COLON;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DELETE_OP;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.PROPERTY_VALUE;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.VALUE;
import static net.flex.dci.otn.topology.cache.utils.DciCacheConstants.ROUTE_STRUCT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otn.controller.db.monitor.core.mapper.CollectionRefNetConfMapper;
import net.flex.dci.otn.controller.db.monitor.core.mapper.GeneralCollectionRefObjectTypeMapper;
import net.flex.dci.otn.controller.db.monitor.core.processor.link.PhyLinkChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.link.SiteLinkChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.link.TunnelChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.AdapterChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.TelemetryChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.CollectionDto;
import net.flex.dci.otn.controller.db.monitor.utils.DocumentTransferUtils;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2021/11/11 13:51
 */
@Slf4j
public abstract class AbstractBodyChangeHandler implements IChangeBodyHandler {

    @Autowired
    protected DciTopologyCacheManager dciTopologyCacheManager;

    @Autowired
    protected RedisCacheOperation redisCacheOperation;

    @Autowired
    protected AdapterChangeDataProcessor adapterChangeDataProcessor;

    @Autowired
    protected TelemetryChangeDataProcessor telemetryChangeDataProcessor;

    @Autowired
    protected SiteLinkChangeDataProcessor siteLinkChangeDataProcessor;

    @Autowired
    protected PhyLinkChangeDataProcessor phyLinkChangeDataProcessor;

    @Autowired
    protected TunnelChangeDataProcessor tunnelChangeDataProcessor;


    public abstract String getCollection();

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        log.info("start to extract  change body :{}", updateDocument.toJson());
        List<ChangeObject> changeObjects = new ArrayList<>();
        //change have one key and one value for updateDocument
        for (Entry<String, Object> entry : updateDocument.entrySet()) {
            String key = entry.getKey();
            ChangeObject changeObject = extractChangeObject(key, sourceDocument, updateDocument);
            changeObjects.add(changeObject);
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
            String collectionName,
            Document sourceDocument,
            Document deleteDoc) {
        log.debug("extract delete change object");
        if (dataStoreType.equals(DataStoreType.OPERATIONAL)) {
            return new ArrayList<>();
        }
        String deleteKeyId = deleteDoc.getString(DELETE_OP);
        removeRefCache(deleteKeyId);
        CollectionDto collectionDto = CollectionRefNetConfMapper.extractColKeyName(collectionName);
        Document changeData = new Document(collectionDto.getObjectKeyName(), deleteKeyId);
        ChangeObject changeObject = new ChangeObject();
        changeObject.setObjectType(collectionDto.getObjectType());
        changeObject.setObjectKeyName(collectionDto.getObjectKeyName());
        changeObject.setChangeBody(changeData);
        return Collections.singletonList(changeObject);
    }

    private void removeRefCache(String deleteKeyId) {
        log.debug("remove ref cache for the deleteKeyId:{}", deleteKeyId);
        dciTopologyCacheManager.removeAsync(deleteKeyId);
        //special for the ochLink route
        if (OchLinkIdNamingRule.isOchBusinessLink(deleteKeyId)) {
            log.info("start to remove the och link route:{}", deleteKeyId);
            String cacheKey = ROUTE_STRUCT + deleteKeyId;
            redisCacheOperation.removeKeyAsync(cacheKey);
        }
    }

    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        return null;
    }

    protected ChangeObject extractChangeObject(ChangeObject changeObject) {
        ChangeObject extractChangeObject = new ChangeObject();
        Document changeDocument = changeObject.getChangeBody();
        Document realChangeBody = new Document();
        for (String key : changeDocument.keySet()) {
            if (key.contains(COLON)) {
                realChangeBody.append(key.split(COLON)[1], changeDocument.get(key));
            } else {
                realChangeBody.append(key, changeDocument.get(key));
            }
        }
        extractChangeObject.setChangeBody(realChangeBody);
        return extractChangeObject;
    }


    protected ChangeObject extractDetailChangeObject(String keyValue, String paths, Boolean pre,
            Document sourceDocument,
            Object updateDocument) {
        Map<String, Object> updateDetails = DocumentTransferUtils.extractPayLoad(
                (Document) updateDocument);
        ChangeObject changeObject = new ChangeObject();
//        String paths = (String) updateDetails.keySet().toArray()[0];
        Object value = updateDetails.get(paths);
        String[] subPaths = paths.split("\\.");
        Object result = value;

        int index = 0;
        for (int i = subPaths.length - 1; i >= 0; i--) {
            if (StringUtils.isNumeric(subPaths[i])) {
                result = Collections.singletonList(result);
            } else {
                if (subPaths[i].contains(COLON)) {
                    String tempKey = subPaths[i].split(COLON)[1];
                    result = new Document(tempKey, result);
                } else {
                    if (subPaths[i].equals(PROPERTY_VALUE)) {
                        result = getPropertyKeyChangeName(paths, sourceDocument);
                        result = new Document(subPaths[i], result);
                    } else {
                        result = new Document(subPaths[i], result);
                    }

                }
            }
            if (subPaths[i].contains(keyValue)) {
                index = pre ? i - 1 : i + 1;
                if (index == subPaths.length) {
                    index = i;
                }
                break;
            }
        }
        Object document = sourceDocument;
        for (int j = 0; j <= index; j++) {
            if (!StringUtils.isNumeric(subPaths[j])) {
                document = ((Document) document).get(subPaths[j]);
            } else {
                List<Object> details = (List<Object>) document;
                if (details.size() > 0) {
                    if (Integer.parseInt(subPaths[j]) >= details.size()) {
                        document = details.get(details.size() - 1);
                    } else {
                        document = details.get(Integer.parseInt(subPaths[j]));
                    }
                }
            }
        }
        Document directDocument = null;
        if (document instanceof ArrayList) {

            directDocument = ((ArrayList<?>) document).isEmpty() ? null
                    : (Document) ((ArrayList<?>) document).get(0);
        } else {
            directDocument = (Document) document;
        }
        if (directDocument != null) {
            for (String key : directDocument.keySet()) {
                if (key.contains("id")) {
                    if (pre) {
                        result = ((Document) result).append(key, directDocument.get(key));
                    } else {
                        List<Document> temp = (List<Document>) ((Document) result).get(keyValue);
                        if (!temp.isEmpty()) {
                            if (temp.get(0) instanceof Document) {
                                Document tempDoc = temp.get(0);
                                tempDoc.append(key, directDocument.get(key));
                                result = tempDoc;
                            }
                        }
                    }

                    break;
                }
            }
        }
        changeObject.setChangeBody((Document) result);
        return changeObject;
    }

    private Object getPropertyKeyChangeName(String paths,
            Document sourceDocument) {
        log.debug("extract the change direct value for the property change paths:{}", paths);
        if (paths.endsWith(VALUE)) {
            paths = paths.replaceFirst("\\.value$", "");
        }
        Object result = null;
        String[] subPaths = paths.split("\\.");
        Object document = sourceDocument;
        for (int index = 0; index < subPaths.length; index++) {
            if (!StringUtils.isNumeric(subPaths[index])) {
                document = ((Document) document).get(subPaths[index]);
            } else {
                List<Object> details = (List<Object>) document;
                if (details.size() > 0) {
                    if (Integer.parseInt(subPaths[index]) >= details.size()) {
                        document = details.get(details.size() - 1);
                    } else {
                        document = details.get(Integer.parseInt(subPaths[index]));
                    }
                }
            }
        }

        result = document;

        return result;
    }

    protected ChangeObject getNodeChangeObject(Document updateDocument) {
        log.debug("extract  generate change object, {}", updateDocument.toJson());
        Map<String, Object> updateDetails = DocumentTransferUtils.extractPayLoad(updateDocument);
        ChangeObject changeObject = new ChangeObject();
        Set<String> keySet = updateDetails.keySet();
        String keyId = null;
        String physical = null;
        for (String key : keySet) {
            if (key.contains(ID)) {
                keyId = key;
            } else if (key.contains(COLON)) {
                physical = key;
            }
        }
        Document physicalDocument = new Document(keyId, updateDocument.get(keyId));
        physicalDocument.append(physical.split(COLON)[1],
                updateDocument.get(physical));
        changeObject.setChangeBody(physicalDocument);
        return changeObject;
    }


    protected ChangeObject getSubNodeChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        log.debug("extract sub  generate change object, {}", updateDocument);
        ChangeObject changeObject = new ChangeObject();
//        Set<String> keySet = sourceDetails.keySet();
//        String keyId = null;
//        for (String key : keySet) {
//            if (key.contains(ID)) {
//                keyId = key;
//                break;
//            }
//        }
        Object changeTopDoc = new Document();
        String[] subPaths = path.split("\\.");
        int index = 0;
        for (int i = 0; i < subPaths.length; i++) {
            if (StringUtils.isNumeric(subPaths[i])) {
                index = i;
                break;
            }
        }

        changeTopDoc = sourceDocument;
        for (int j = 0; j <= index; j++) {
            if (!StringUtils.isNumeric(subPaths[j])) {
                changeTopDoc = ((Document) changeTopDoc).get(subPaths[j]);
            } else {
                List<Object> details = (List<Object>) changeTopDoc;
                changeTopDoc = details.get(Integer.parseInt(subPaths[j]));
            }
        }

        Document directDocument = (Document) changeTopDoc;
        Document changeDoc = null;
        for (String key : directDocument.keySet()) {
            if (key.contains("id")) {
                changeDoc = new Document(key, directDocument.get(key));
                break;
            }
        }
        if (subPaths[index + 1].contains(COLON)) {
            changeDoc.append(subPaths[index + 1].split(COLON)[1], updateDocument.get(path));
        } else {
            changeDoc.append(subPaths[index + 1], updateDocument.get(path));
        }
        changeObject.setChangeBody(changeDoc);
        return changeObject;
    }

    protected void discardCache(List<String> ids) {
        log.debug("discard the cache for the ids:{}", ids);
        CompletableFuture.runAsync(() -> {
            ids.forEach(dciTopologyCacheManager::remove);
        });
    }

    protected void discardCache(String id) {
        log.debug("discard the cache for the id:{}", id);
        CompletableFuture.runAsync(() -> {
            dciTopologyCacheManager.remove(id);
        });
    }

}
