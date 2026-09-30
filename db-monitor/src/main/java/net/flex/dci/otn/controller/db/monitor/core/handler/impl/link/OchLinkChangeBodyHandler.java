/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl.link;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OCH_LINK_COLLECTION;
import static net.flex.dci.otn.topology.cache.utils.DciCacheConstants.ROUTE_STRUCT;

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
public class OchLinkChangeBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return OCH_LINK_COLLECTION;
    }

    @Override
    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        log.debug("start to extract change object for the och link, change object is :{}",
                updateDocument.toJson());
        ChangeObject changeObject = new ChangeObject();
        if (path.equals("data.link.0")) {
            changeObject = getNodeChangeObject((Document) updateDocument.get(path));
            removeOchLinkCache(changeObject, sourceDocument);
            changeObject.setObjectType(ObjectType.Link.name());
        } else if (path.contains("och-topology:och")) {
            changeObject = extractDetailChangeObject("och-topology:och", path, true, sourceDocument,
                    updateDocument);
            changeObject.setObjectType(ObjectType.Link.name());
        }

        if (changeObject.getChangeBody() == null || changeObject.getChangeBody().isEmpty()) {
            return null;
        }
        return changeObject;
    }

    /**
     * refresh the ochLink cache
     *
     * @param changeObject
     */
    private void removeOchLinkCache(ChangeObject changeObject, Document sourceDoc) {
        log.debug("remove the och link cache asy,the key is:{}", changeObject.getObjectKeyName());
        Document ochLinkChangeBody = changeObject.getChangeBody();
        String ochLinkId = ochLinkChangeBody.getString("link-id");
        Document ochTopologyOch = (Document) ochLinkChangeBody.get("och");

        boolean hasExplicitRoute = false;
        if (ochTopologyOch != null) {
            hasExplicitRoute = ochTopologyOch.containsKey("explict-route");
        }
        if (hasExplicitRoute) {
            log.info("refresh the och link route remove och link route cache:{}", ochLinkId);
            String cacheKey = ROUTE_STRUCT + ochLinkId;
            redisCacheOperation.removeKeyAsync(cacheKey);
        }

    }
}
