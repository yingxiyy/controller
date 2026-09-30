package net.flex.dci.otc.controller.notifier.core.listener;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ObjectChangeMessage;
import net.flex.dci.otc.controller.notifier.core.domain.objectdetail.ObjectDetailNotification;
import net.flex.dci.otc.controller.notifier.core.domain.objectdetail.ObjectNotification;
import net.flex.dci.otc.controller.notifier.core.domain.objectdetail.OtnNotification;
import net.flex.dci.otc.controller.notifier.core.websocket.WebSocketServerManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @version 1.0
 * @date 2021/11/5 16:01
 */

@ConditionalOnClass({WebMvcConfigurer.class, WebSocketServerManager.class})
@Component
@Slf4j
public class ObjectUpdateNotificationListener implements MessageListener<String, String> {

    @Autowired
    private WebSocketServerManager webSocketServerManager;

    private Gson gson;

    public ObjectUpdateNotificationListener() {
        gson = new Gson();
    }


    @SneakyThrows
    @Override
    @Log
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("start to handle the object change notification event");
        ObjectChangeMessage message = gson.fromJson(record.value(), ObjectChangeMessage.class);
        log.info("start to send object notification message: {}", message);
        ObjectNotification objectDetailNotification = buildObjectNotification(message);
        webSocketServerManager.sendObjectChangeMessage(gson.toJson(objectDetailNotification));
        log.debug("end to handle the object change notification event");
    }

    private ObjectNotification buildObjectNotification(ObjectChangeMessage message) {
        log.debug("start to build object notification for message {}", message);
        Gson gson = new Gson();
        Object changeObject = gson.fromJson(message.getData(), (Type) Object.class);
        return ObjectNotification.builder()
                .objectDetailNotification(
                        ObjectDetailNotification.builder()
                                .otnNotification(OtnNotification.builder()
                                        .body(changeObject)
                                        .objectType(message.getObjectType())
                                        .eventType(message.getEventType())
                                        .topologyRef(message.getTopologyRef())
                                        .topologyType(message.getTopologyType())
                                        .build()).build()).build();
    }
}
