package net.flex.dci.otc.controller.notifier.core.listener;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otc.controller.notifier.core.domain.element.ElementChange;
import net.flex.dci.otc.controller.notifier.core.domain.element.ElementChangeBody;
import net.flex.dci.otc.controller.notifier.core.websocket.WebSocketServerManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/5 14:06
 */
@Component
@Slf4j
public class ElementChangeNotificationListener implements MessageListener<String, String> {

    @Autowired
    private WebSocketServerManager webSocketServerManager;

    private final Gson gson;

    public ElementChangeNotificationListener() {
        gson = new Gson();
    }

    @SneakyThrows
    @Override
    @Log
    public void onMessage(ConsumerRecord<String, String> stringStringConsumerRecord) {
        log.info("start to handle the element change notification");
        try {
            String record = stringStringConsumerRecord.value();
            ElementChangeNotification elementChangeNotification = gson.fromJson(record,
                    ElementChangeNotification.class);
            ElementChange elementChange = buildElementChangeNotification(elementChangeNotification);
            webSocketServerManager.sendElementChangeMessage(gson.toJson(elementChange));
        } catch (Exception ex) {
            log.error("failed to handle the notification,the reason is:{}", ex.getMessage(), ex);
        }
    }

    private ElementChange buildElementChangeNotification(
            ElementChangeNotification elementChangeNotification) {
        return ElementChange.builder()
                .elementChangeBody(ElementChangeBody.builder()
                        .content(elementChangeNotification.getContent())
                        .changeType(elementChangeNotification.getChangeType().name())
                        .elementType(elementChangeNotification.getElementType().name()).build())
                .build();
    }
}
