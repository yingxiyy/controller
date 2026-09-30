/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.handler.impl;

import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SFTP_SERVER_COLLECTION;

import java.util.List;
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
public class FtpChangeBodyHandler extends AbstractBodyChangeHandler {

    @Override
    public String getCollection() {
        return SFTP_SERVER_COLLECTION;
    }

    @Override
    public List<ChangeObject> extractSetChangeObject(Document sourceDocument,
            Document updateDocument) {
        log.debug("start to handle sftp change object {}", updateDocument.toJson());

        return null;
    }


}
