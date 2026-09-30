package net.flex.dci.otn.controller.db.monitor.core.service.handler;

import static io.debezium.connector.mongodb.SourceInfo.COLLECTION;
import static io.debezium.data.Envelope.FieldName.AFTER;
import static io.debezium.data.Envelope.FieldName.SOURCE;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otn.controller.db.monitor.core.NetConfEventChangeHandlerDispatcher;
import net.flex.dci.otn.controller.db.monitor.core.service.NotificationService;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import org.apache.kafka.connect.data.Struct;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2021/11/4 10:25
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataCreateHandler implements IChangeHandler {


    private final NotificationService notificationService;

    private final NetConfEventChangeHandlerDispatcher netConfEventChangeHandlerDispatcher;

    @Override
    public void handle(Map<String, Object> payload)
            throws InvocationTargetException, IllegalAccessException {
        log.debug("start to handle the data create event ,pay load is {}", payload);
        String addData = (String) payload.get(AFTER);
        Document document = Document.parse(addData);
        Struct dbChangeDetail = (Struct) payload.get(SOURCE);
        handleCreateEvent(document, dbChangeDetail);
    }

    /***
     *
     * @param document
     * @param dbChangeDetail
     */
    private void handleCreateEvent(Document document, Struct dbChangeDetail) {
        log.debug("start to handle the data create for the pay load :{},source is {}", document,
                dbChangeDetail);
        String collectionName = (String) dbChangeDetail.get(COLLECTION);
        List<NetConfEventChangeDto> eventChangeDtos = netConfEventChangeHandlerDispatcher.extractCreateInfo(
                collectionName,
                document);
        if (CollectionUtils.isEmpty(eventChangeDtos)) {
            return;
        }
        notificationChange(eventChangeDtos);
    }

    private void notificationChange(List<NetConfEventChangeDto> changeDtos) {
        changeDtos.forEach(
                eventChangeDto -> {
                    if (eventChangeDto.getDataStoreType().equals(DataStoreType.CONFIG)) {
                        notificationService.publishNotification(EventType.Add, eventChangeDto);
                    } else {
                        notificationService.publishNotification(EventType.Update, eventChangeDto);
                    }
                });
    }
}
