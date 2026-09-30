/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.controller.nms.exceptions.JSONParseException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * serialize the or deserialize input
 *
 * @date: 2021/4/26
 */
@Slf4j
public class SerializeUtils {

//    private  JsonUtil jsonUtil;


    public static String serializeDataObject(String namespace, String cmd, DataObject dataObject)
            throws CommonException {
        JsonUtil jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);
        return jsonUtil.fromDataObjectToJson(namespace, cmd,
                dataObject, false);
    }


    public static DataObject parseRpcInput(String namespace, String cmd, String requestBody)
            throws JSONParseException {
        try {
            JsonUtil jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);

            return jsonUtil
                    .fromJsonToDataObject(namespace, cmd, requestBody, true);
        } catch (Exception ex) {
            throw new JSONParseException("the request body input for the nms is invalid");
        }
    }

    public static InstanceIdentifier<?> fromString2InstanceIdentitfier(String identifier) {
        JsonUtil jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);

        return jsonUtil.fromStringToInstanceIdentifier(identifier);
    }

    public static String serializeDataObject2Json(String identifier, DataObject dataObject) {
        JsonUtil jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);

        return jsonUtil.fromDataObjectToJson(identifier, dataObject);
    }

    public static DataObject serializeDataObject(String identifier, String requestBody) {
        JsonUtil jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);

        return jsonUtil.fromJsonToDataObject(identifier, requestBody);
    }


}
