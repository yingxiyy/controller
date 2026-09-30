package net.flex.dci.otn.controller.db.monitor.core.service;

import io.debezium.data.Envelope.Operation;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.service.handler.DataCreateHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.handler.DataUpdateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/4 10:21
 */
@Component
@Slf4j
public class DataChangeService {

    @Autowired
    private DataCreateHandler dataCreateHandler;

    @Autowired
    private DataUpdateHandler dataUpdateHandler;

    /**
     * handle data change event
     *
     * @param operation
     * @param payload
     */
    public void handlerDataChange(Operation operation, Map<String, Object> payload) {

        log.info("start to handle the data change handler,the operation is {}", operation);
        try {
            if (operation.equals(Operation.CREATE)) {
                dataCreateHandler.handle(payload);
            } else if (operation.equals(Operation.UPDATE)) {
                dataUpdateHandler.handle(payload);
            }
        } catch (Exception exception) {
            log.error("failed to handle the data change {} cause:{}", exception.getMessage(),
                    exception.getCause(), exception);
//            log.error("failed to handle the change", exception.getCause());
        }
    }
}
