package net.flex.dci.otn.controller.implement.common.recorder;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otn.controller.implement.common.lifecycle.TaskInfo;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LifeCycleSeviceNew {
//  @Autowired
//  private TaskInfoKafkaService kafkaService;

    public void createSiteLinkTask(String linkId, String linkName, ActionType actionType,
            String who) {
        log.info("create SiteLink TaskInfo {} {} {}", linkId, linkName, actionType);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                who,
                ResourceType.siteLink,
                actionType,
                null);
        taskInfoMessage.setResourceId(linkId);
        taskInfoMessage.setResourceName(linkName);

        TaskInfo info = new TaskInfo();
        info.setResourceId(linkId);
        info.setActionType(actionType);
        info.setResourceName(linkName);
        info.setResourceType(ResourceType.siteLink);
        info.setActionTime(taskInfoMessage.getActionTime());

//		taskInfoMessage.setDetail(resultLinks2Json(resultLinks));
//    taskInfoMessage.setResourceName(dbLink.getAugmentation(Link1.class).getSite().getFriendlyName());
        taskInfoMessage.setSuccessfully(false);

        sendMsg(taskInfoMessage);
    }

    /**
     * publish message to life cycle module to save date in mysql
     *
     * @param msg
     */
    private void sendMsg(TaskInfoMessage msg) {
        log.info("send msg");
        TaskInfoMessager.sendMessage(msg);
    }

//
//  private String resultLinks2Json(ResultLinks resLinks) {
//    try {
//      return objMapper.writeValueAsString(resLinks);
//    } catch (JsonProcessingException e) {
//      log.error("fail to convert ResultLinks to json", e);
//      return "";
//    }
//  }
}
