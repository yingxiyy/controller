/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.node;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.ChangeObjectUtils;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_NODE_COLLECTION;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.REMOVE_ATTRIBUTES;

/**
 * @version 1.0
 * @date 2021/11/15 11:18
 */
@Slf4j
@Component
public class ViewNodeChangeDataBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return VIEW_NODE_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
                                                     Document updateDocument) {
        log.info("start to handle the change for the site node ");
        log.debug("start to handle the change object for the site node change object");
        ChangeObject changeObject = new ChangeObject();
        if (updateDocument.size() == 1) {
            for (Entry<String, Object> entry : updateDocument.entrySet()) {
                String key = entry.getKey();
                if (key.equals("data.node.0")) {
                    changeObject = getNodeChangeObject((Document) updateDocument.get(key));

                    break;

                } else if (key.contains("view-topology:view")) {
                    changeObject = extractDetailChangeObject("view-topology:view", key, true,
                            sourceDocument,
                            updateDocument);
                    break;
                }
            }
        } else {
            //to do change view node coordinate system
            List<ChangeObject> coordinateChange = new ArrayList<>();
            for (Entry<String, Object> entry : updateDocument.entrySet()) {
                String key = entry.getKey();
                ChangeObject posChangeObject = extractDetailChangeObject("view-topology:view", key,
                        true,
                        sourceDocument,
                        updateDocument);
                coordinateChange.add(posChangeObject);
            }
            changeObject = mergeCoordinateChangeObject(coordinateChange);
        }
        changeObject.setObjectType(ObjectType.Node.name());
        if (changeObject.getChangeBody() == null || changeObject.getChangeBody().isEmpty()) {
            return null;
        }
        return Collections.singletonList(changeObject);
    }

    @Override
    public ChangeObject extractUNSETChangeObject(Document sourceDocument, Document unsetDocument) {
        log.info("start to extract the unset attribute {}", unsetDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        for (Map.Entry<String, Object> entry : unsetDocument.entrySet()) {
            String key = entry.getKey();
            Boolean value = (Boolean) entry.getValue();
            if (value) {
                String[] subPaths = key.split("\\.");
                String unsetAttribute = subPaths[subPaths.length - 1];
                Document unsetDoc = new Document(REMOVE_ATTRIBUTES, unsetAttribute);
                changeObject = ChangeObjectUtils.buildUnsetObject(sourceDocument, subPaths, unsetDoc);
                if (changeObject != null) {
                    changeObject.setObjectType(ObjectType.Node.name());
                }

            }
        }
        return changeObject;
    }

    private ChangeObject mergeCoordinateChangeObject(List<ChangeObject> coordinateChange) {
        log.debug("merge two change object to one change object ,coordinate change is {}",
                coordinateChange);
        ChangeObject changeObject = new ChangeObject();
        Document updateDoc = null;
        for (ChangeObject cj : coordinateChange) {
            Document changeDoc = cj.getChangeBody();
            updateDoc = mergeChangeDoc(updateDoc, changeDoc);
        }

        changeObject.setChangeBody(updateDoc);
        return changeObject;
    }

    private Document mergeChangeDoc(Document updateDoc, Document changeDoc) {
        if (updateDoc == null) {
            return changeDoc;
        }
        for (Entry<String, Object> entry : changeDoc.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (updateDoc.containsKey(key)) {
                if (value instanceof Document && updateDoc.get(key) instanceof Document) {
                    ((Document) updateDoc.get(key)).putAll((Document) value);
                }
            }
        }
        return updateDoc;
    }
}
