package net.flex.dci.otc.controller.notifier.core.listener;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otc.controller.notifier.core.domain.broadcast.BroadcastNotification;
import net.flex.dci.otc.controller.notifier.core.domain.broadcast.MessageNotification;
import net.flex.dci.otc.controller.notifier.core.websocket.WebSocketServerManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@ConditionalOnClass({WebMvcConfigurer.class, WebSocketServerManager.class})
@Component
@Slf4j
public class BroadcastMessageKafkaListener implements MessageListener<String, String> {

    private final static String SUCCESS = "success";

    private final static String ERROR = "error";

    @Autowired
    private WebSocketServerManager webSocketServerManager;

    private Gson gson;

    public BroadcastMessageKafkaListener() {
        gson = new Gson();
    }

    @SneakyThrows
    @Override
    @Log
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("start to handle the broadcast notification event");
        BroadcastMessage message = gson.fromJson(record.value(), BroadcastMessage.class);
        log.debug("start to send broadcast notification message: {}", message);
        BroadcastNotification broadcastNotification = buildBroadCastNotification(message);
        webSocketServerManager.sendBroadcastMessage(gson.toJson(broadcastNotification));

    }

    private BroadcastNotification buildBroadCastNotification(BroadcastMessage message) {
        log.debug("start to build broadcast notification for message {}", message);
        String status = getMessageStatus(message);
        return BroadcastNotification.builder().messageNotification(MessageNotification.builder()
                .message(message.getMessage())
                .title(message.getTitle())
                .status(status)
                .time(DataTimeConvert.now())
                .build()).build();
    }

    private String getMessageStatus(BroadcastMessage message) {
        if (message.isError()) {
            return ERROR;
        }
        return SUCCESS;
    }

//    private MessageNotification buildMessageNotification(String message) {
//        log.debug("start to build message notification {}", message);
//        return MessageNotification.builder()
//                .messageBody(MessageBody.builder()
//                        .time(DataTimeConvert.now())
//                        .content(message)
//                        .build())
//                .build();
//    }
}
