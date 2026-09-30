/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SCHEDULES_COLLECTION;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/11 10:33
 */
@Slf4j
@Component
public class ScheduleChangeBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return SCHEDULES_COLLECTION;
    }

    @Override
    protected ChangeObject extractChangeObject(String path, Document sourceDocument,
            Document updateDocument) {
        return null;
    }


}
