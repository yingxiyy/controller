package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;

/**
 * @version 1.0
 * @date 2022/4/4 21:00
 */
@Slf4j
public class TaskInfoMessager {

    //    private static final String TASK_INFO_TOPIC = "test";
    @Setter
    private static PublishService publishService;


    public static void sendMessage(Object message) {
        publishService.send(Constant.TASKINFO_TOPIC, message);
    }

    public static void sendMessage(Object message, String key) {
        publishService.send(Constant.TASKINFO_TOPIC, key, message);
    }

    public static void sendMessage(TaskInfoMessage taskInfoMessage) {
        String key = taskInfoMessage.getResourceId();
        publishService.send(Constant.TASKINFO_TOPIC, key, taskInfoMessage);
    }

    public static void sendMessage(TaskInfoMessage taskInfoMessage, String key) {
        publishService.send(Constant.TASKINFO_TOPIC, key, taskInfoMessage);
    }

    public static void initializeTopics() {
        publishService.newTopic(Constant.TASKINFO_TOPIC);
    }


}
