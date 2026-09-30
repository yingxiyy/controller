package net.flex.dci.otn.controller.pm;

import lombok.Setter;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;

public class KafkaMessage {

    //    private static final String TASK_INFO_TOPIC = "test";
    @Setter
    private static PublishService publishService;


    public static void sendMessage(Object message) {
        publishService.send(Constant.TASKINFO_TOPIC, message);
    }

    public static void sendMessage(TaskInfoMessage taskInfoMessage) {
        publishService.send(Constant.TASKINFO_TOPIC, taskInfoMessage);
    }

    public static void initializeTopics() {
        publishService.newTopic(Constant.TASKINFO_TOPIC);
    }


}
