/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl;

import java.util.List;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;

/**
 * @version 1.0
 * @date 2021/11/12 14:47
 */
public class DefaultBodyChangeHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return ObjectType.Event.name();
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        return null;
    }

    @Override
    public List<ChangeObject> extractCreateObject(String collectionName, Document document) {
        return null;
    }

}
