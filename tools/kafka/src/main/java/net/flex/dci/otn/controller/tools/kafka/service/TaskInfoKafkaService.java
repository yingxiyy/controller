package net.flex.dci.otn.controller.tools.kafka.service;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class TaskInfoKafkaService {

    @Autowired
    PublishService publishService;

    @PostConstruct
    public void init() {
        publishService.newTopic(Constant.TASKINFO_TOPIC);
    }

    public void sendMessage(TaskInfoMessage taskInfoMessage) {
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        publishService.send(Constant.TASKINFO_TOPIC, taskInfoMessage);
    }
}
