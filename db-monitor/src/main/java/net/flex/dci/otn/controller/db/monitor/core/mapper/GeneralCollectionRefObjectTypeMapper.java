/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.mapper;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OCH_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OCH_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SCHEDULES_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SFTP_SERVER_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TUNNEL_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_NODE_COLLECTION;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;

/**
 * @version 1.0
 * @date 2021/11/17 15:28
 */

public class GeneralCollectionRefObjectTypeMapper {

    private final static Map<String, String> collectionGeneralObjectTypeRef = new HashMap<>();

    static {
        //node change handler
        collectionGeneralObjectTypeRef.put(PHY_NODE_COLLECTION, ObjectType.Node.name());
        collectionGeneralObjectTypeRef.put(SITE_NODE_COLLECTION, ObjectType.Node.name());
        collectionGeneralObjectTypeRef.put(VIEW_NODE_COLLECTION, ObjectType.Node.name());
        collectionGeneralObjectTypeRef.put(OCH_NODE_COLLECTION, ObjectType.Node.name());
        //link change handler
        collectionGeneralObjectTypeRef.put(PHY_LINK_COLLECTION, ObjectType.Link.name());
        collectionGeneralObjectTypeRef.put(SITE_LINK_COLLECTION, ObjectType.Link.name());
        collectionGeneralObjectTypeRef.put(VIEW_LINK_COLLECTION, ObjectType.Link.name());
        collectionGeneralObjectTypeRef.put(OCH_LINK_COLLECTION, ObjectType.Link.name());
        //tunnel change handler
        collectionGeneralObjectTypeRef.put(TUNNEL_COLLECTION, ObjectType.Tunnel.name());
        //ftp
        collectionGeneralObjectTypeRef.put(SFTP_SERVER_COLLECTION, ObjectType.Ftp.name());
        //schedule
        collectionGeneralObjectTypeRef.put(SCHEDULES_COLLECTION, ObjectType.Schedule.name());

    }

    public static String getGeneralObjectType(String collectionName) {
        return collectionGeneralObjectTypeRef.getOrDefault(collectionName, null);
    }

}
