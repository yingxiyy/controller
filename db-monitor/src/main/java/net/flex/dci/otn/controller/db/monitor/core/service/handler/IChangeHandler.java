package net.flex.dci.otn.controller.db.monitor.core.service.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @version 1.0
 * @date 2021/11/4 12:21
 */
public interface IChangeHandler {

    /**
     * handle change event payload
     *
     * @param payload
     */
    void handle(Map<String, Object> payload)
            throws InvocationTargetException, IllegalAccessException;
}
