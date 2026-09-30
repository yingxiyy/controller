package net.flex.dci.otc.controller.otdr.service;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.serialization.JsonUtil;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 11:18
 */
@Slf4j
@Component
public abstract class BaseService {

    @Autowired
    protected JsonUtil jsonUtil;

    /**
     * form rpc input json string to data object
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    protected <T> T formRpcInput(String json, Class<T> clazz) {
        return (T) jsonUtil.fromJsonToDataObject(json, true);
    }

    protected <T> T formRpcOutput(String module, String operation, String json, Class<T> clazz) {
        return (T) jsonUtil.fromJsonToDataObject(module, operation, json, false);
    }

    /**
     * form rpc output to json
     *
     * @param dataObject
     * @return
     */
    protected String formRpcOutput(DataObject dataObject) {
        return jsonUtil.fromDataObjectToJson(dataObject, true);
    }

}
