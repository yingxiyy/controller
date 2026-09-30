/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TUNNEL_COLLECTION;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TUNNEL_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.mapper.GeneralCollectionRefObjectTypeMapper;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.DocumentTransferUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/11 10:32
 */
@Slf4j
@Component
public class TunnelChangeBodyHandler extends AbstractBodyChangeHandler {

//    @Override
//    public ChangeObject extractSetChangeObject(Document sourceDocument, Document updateDocument) {
//        log.info("start to extract tunnel body change body :{}", updateDocument);
//        ChangeObject changeObject = null;
//        for (Entry<String, Object> entry : updateDocument.entrySet()) {
//            String key = entry.getKey();
//            if (key.equals("data.tunnel:tunnel.0")) {
//                changeObject = getNodeChangeObject(updateDocument);
//                changeObject.setObjectType(ObjectType.Tunnel.name());
//                break;
//            } else {
//                String keyValue = getTunnelTopChangeAttr(key);
//                changeObject = extractDetailChangeObject(keyValue, true, sourceDocument,
//                        updateDocument);
//                changeObject.setObjectType(ObjectType.Tunnel.name());
//                break;
//            }
//        }
//        return changeObject;
//    }

    private String getTunnelTopChangeAttr(String path) {
        log.debug("find the top change attribute  for the tunnel,change path is :{}", path);
        boolean flag = false;
        String result = path;
        String[] subPaths = path.split("\\.");
        for (int i = 0; i < subPaths.length; i++) {
            String subPath = subPaths[i];
            if (flag) {
                result = subPath;
                break;
            }
            if (StringUtils.isNumeric(subPath)) {
                flag = true;
            }
        }
        return result;
    }


    @Override
    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        log.info("tunnel change path is :{}", path);
        ChangeObject changeObject = new ChangeObject();
        String tunnelId = sourceDocument.getString("tunnelId");
        if (path.equals("data.tunnel:tunnel.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(path));
            changeObject.getChangeBody().append(TUNNEL_ID, tunnelId);
            changeObject.setObjectType(ObjectType.Tunnel.name());
        } else {
            String keyValue = getTunnelTopChangeAttr(path);
            changeObject = extractDetailChangeObject(keyValue, path, true, sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Tunnel.name());
        }

        if (changeObject.getChangeBody() == null || changeObject.getChangeBody().isEmpty()) {
            return null;
        }
        ChangeObject enrichChangeObject = tunnelChangeDataProcessor.enrichChangeObject(
                changeObject);
        return enrichChangeObject;
    }


    @Override
    protected ChangeObject getNodeChangeObject(Document updateDocument) {
        log.debug("extract  generate tunnel change object, {}", updateDocument.toJson());
        Map<String, Object> updateDetails = DocumentTransferUtils.extractPayLoad(updateDocument);
        ChangeObject changeObject = new ChangeObject();
        Object[] keys = updateDetails.keySet().toArray();
//        String key = (String) keys[0];
//        Object changeDoc = updateDocument.get(key);
        Document physicalDocument = updateDocument;
        changeObject.setChangeBody(physicalDocument);
        return changeObject;
    }


    @Override
    public String getCollection() {
        return TUNNEL_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractCreateObject(String collectionName, Document createDoc) {
        log.info("start to extract och node create body :{}", createDoc.toJson());
        ChangeObject changeObject = null;
        if (!createDoc.isEmpty()) {
            changeObject = new ChangeObject();
            changeObject.setChangeBody(createDoc);
            changeObject.setObjectType(
                    GeneralCollectionRefObjectTypeMapper.getGeneralObjectType(collectionName));
            ChangeObject enrichChangeObject = tunnelChangeDataProcessor.enrichChangeObject(
                    changeObject);
            return Collections.singletonList(enrichChangeObject);
        }
        return null;
    }
}
