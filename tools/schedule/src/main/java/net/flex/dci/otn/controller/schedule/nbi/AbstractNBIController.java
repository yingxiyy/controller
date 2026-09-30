/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.schedule.nbi;

import net.flex.dci.otc.serialization.JsonUtil;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/10 16:13
 */
@RestController
public abstract class AbstractNBIController {

    @Autowired
    private JsonUtil jsonUtil;


    protected <T> T formRpcInput(String json, Class<T> clazz) {
        return (T) jsonUtil.fromJsonToDataObject(json, true);
    }

    protected String formRpcOutput(DataObject dataObject) {
        return jsonUtil.fromDataObjectToJson(dataObject, true);
    }

}
