/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.link;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_LINK_COLLECTION;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/16 13:17
 */
@Slf4j
@Component
public class PhyLinkChangeBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return PHY_LINK_COLLECTION;
    }

    @Override
    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        log.debug("start to extract change object for the phy link.change object is :{}",
                updateDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        if (path.equals("data.link.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(path));
            changeObject.setObjectType(ObjectType.Link.name());
        } else if (path.contains("otn-phy-topology:physical")) {
            changeObject = extractDetailChangeObject("otn-phy-topology:physical", path, true,
                    sourceDocument, updateDocument);
            changeObject.setObjectType(ObjectType.Link.name());
        }
        if (changeObject.getChangeBody() == null || changeObject.getChangeBody().isEmpty()) {
            return null;
        }
        phyLinkChangeDataProcessor.enrichChangeObject(changeObject);
        return changeObject;
    }


    @Override
    public List<ChangeObject> extractCreateObject(String collectionName, Document createDoc) {
        log.debug("extract site link create change object");
        List<ChangeObject> changeObjects = super.extractCreateObject(collectionName, createDoc);
        List<ChangeObject> richChangeObject = changeObjects.stream()
                .map(changeObject -> phyLinkChangeDataProcessor.enrichChangeObject(
                        changeObject)).collect(Collectors.toList());
        return richChangeObject;
    }
}
