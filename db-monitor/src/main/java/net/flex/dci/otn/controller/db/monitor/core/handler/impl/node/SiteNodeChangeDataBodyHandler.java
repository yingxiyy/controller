/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.node;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_NODE_COLLECTION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/15 11:20
 */
@Slf4j
@Component
public class SiteNodeChangeDataBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return SITE_NODE_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        log.info("start to handle the change for the site node ");
        log.debug("start to handle the change object for the site node change object");
        List<ChangeObject> changeObjects = new ArrayList<>();
        for (Entry<String, Object> entry : updateDocument.entrySet()) {
            String key = entry.getKey();
            ChangeObject changeObject = extractSetChangeObject(key, sourceDocument, updateDocument);
            if (changeObject.getChangeBody() != null && !changeObject.getChangeBody().isEmpty()) {
                changeObjects.add(changeObject);
            }
        }

        return changeObjects;
    }

    private ChangeObject extractSetChangeObject(String key, Document sourceDocument,
            Document updateDocument) {

        ChangeObject changeObject = new ChangeObject();
        if (key.equals("data.node.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(key));
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;
        } else if (key.equals("data.node.0.site-topology:site.supporting-rack")) {
            changeObject = getSubNodeChangeObject(key, sourceDocument, updateDocument);
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;
        } else if (key.contains("supporting-rack")) {
            changeObject = extractDetailChangeObject("supporting-rack", key, false,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Rack.name());
            return changeObject;
        } else if (key.contains("termination-point")) {
            changeObject = extractDetailChangeObject("termination-point", key, false,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Tp.name());
            return changeObject;
        } else if (key.contains("site-topology:site")) {
            changeObject = extractDetailChangeObject("site-topology:site", key, true,
                    sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Node.name());
            return changeObject;
        }
        return changeObject;
    }


}
