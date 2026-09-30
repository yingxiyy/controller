/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.node;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_NODE_COLLECTION;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.NE_ID;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.REMOVE_ATTRIBUTES;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.CrossConnectionChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.EquipmentChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.PhyNodeChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.processor.node.TerminationPointChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.controller.db.monitor.utils.ChangeObjectUtils;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/11 10:32
 */
@Slf4j
@Component
public class PhyNodeChangeBodyHandler extends AbstractBodyChangeHandler {

    @Autowired
    private PhyNodeChangeDataProcessor phyNodeChangeDataProcessor;

    @Autowired
    private CrossConnectionChangeDataProcessor crossConnectionChangeDataProcessor;

    @Autowired
    private TerminationPointChangeDataProcessor terminationPointChangeDataProcessor;

    @Autowired
    private EquipmentChangeDataProcessor equipmentChangeDataProcessor;


    @Override
    public String getCollection() {
        return PHY_NODE_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        log.info("start to handle the change object for the phy node change object");
        log.debug("start to handle the change object for the phy node change object");
        List<ChangeObject> changeObjects = new ArrayList<>();
        for (Entry<String, Object> entry : updateDocument.entrySet()) {
            String key = entry.getKey();
            ChangeObject changeObject = extractSetChangeObject(key, sourceDocument, updateDocument);
            if (changeObject.getChangeBody() != null && !changeObject.getChangeBody().isEmpty()) {
                //todo: remove the node cache
                List<ChangeObject> changeObjectList = new ArrayList<>();
                if (changeObject.getObjectType().equals(ObjectType.Node.name())) {
                    changeObjectList = phyNodeChangeDataProcessor.processUpdateObject(changeObject);
                } else if (changeObject.getObjectType().equals(ObjectType.Tp.name())) {

                    changeObjectList = terminationPointChangeDataProcessor.processUpdateObject(
                            changeObject, sourceDocument);
                } else if (changeObject.getObjectType().equals(ObjectType.Equip.name())) {
                    changeObjectList = equipmentChangeDataProcessor.processUpdateObject(
                            changeObject, null);
                } else if (changeObject.getObjectType().equals(ObjectType.CrossConnection.name())) {
                    changeObjectList = crossConnectionChangeDataProcessor.processChangeEvent(
                            changeObject);
                }
                changeObjects.addAll(changeObjectList);
            }

        }

        return changeObjects;
    }


    @Override
    public ChangeObject extractUNSETChangeObject(Document sourceDocument, Document unsetDocument) {
        log.info("start to extract the unset attribute {}", unsetDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        for (Entry<String, Object> entry : unsetDocument.entrySet()) {
            String key = entry.getKey();
            Boolean value = (Boolean) entry.getValue();
            if (value) {
                String[] subPaths = key.split("\\.");
                String unsetAttribute = subPaths[subPaths.length - 1];
                Document unsetDoc = new Document(REMOVE_ATTRIBUTES, unsetAttribute);
                changeObject = ChangeObjectUtils.buildUnsetObject(sourceDocument, subPaths,
                        unsetDoc);
                if (changeObject != null) {
                    phyNodeChangeDataProcessor.processUnsetAttribute(sourceDocument,
                            unsetAttribute);
                    changeObject.setObjectType(ObjectType.Node.name());
                }

            }
        }
        return changeObject;
    }


    private ChangeObject extractSetChangeObject(String key, Document sourceDocument,
            Document updateDocument) {
        ChangeObject changeObject = new ChangeObject();
        if (key.equals("data.node.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(key));
            phyNodeChangeDataProcessor.onImplementStateChanged(sourceDocument, updateDocument, key);
            String nodeId = sourceDocument.getString(NE_ID);
            changeObject.setNeId(nodeId);
            changeObject = phyNodeChangeDataProcessor.enrichChangeObject(changeObject);
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;

        } else if (key.equals("data.node.0.termination-point")) {
            changeObject = getSubNodeChangeObject("data.node.0.termination-point",
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;
        } else if (key.contains("termination-point")) {
            changeObject = extractDetailChangeObject("termination-point", key, false,
                    sourceDocument,
                    updateDocument);
            changeObject = extractChangeObject(changeObject);
            changeObject.setObjectType(ObjectType.Tp.name());
            return changeObject;
        } else if (key.contains("cross-connections")) {
            changeObject = extractDetailChangeObject("cross-connections", key, false,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.CrossConnection.name());
            return changeObject;
        } else if (key.contains("internal-links")) {
            changeObject = extractDetailChangeObject("internal-links", key, false,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.InternalLink.name());
            return changeObject;
        } else if (key.contains("equipments")) {
            changeObject = extractDetailChangeObject("equipments", key, false, sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Equip.name());
            return changeObject;
        } else if (key.contains("otn-phy-topology:physical")) {
            changeObject = extractDetailChangeObject("otn-phy-topology:physical", key, true,
                    sourceDocument,
                    updateDocument);
            phyNodeChangeDataProcessor.onImplementStateChanged(sourceDocument, updateDocument, key);
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;
        }
        return changeObject;
    }


}
