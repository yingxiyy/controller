package net.flex.dci.otn.controller.discovery.common.util;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;

@Slf4j
public class TaskInfo {

    private static Gson gson = new Gson();
    private TaskInfoMessage taskInfoMessage;

    public TaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
    }


    public void logMessage(TunnelDiscoveryResult result) {
        String msg = "业务 设备配置信息同步 ";

        log.debug("{}", gson.toJson(result));

        taskInfoMessage.setResourceId(result.getTunnelId());
        taskInfoMessage.setResourceName(result.getFriendlyName());
        taskInfoMessage.setSuccessfully(result.getFinalState().equals(ImplementState.Implement));
        taskInfoMessage.setDetail(gson.toJson(result));
        taskInfoMessage.setErrorReason("资源匹配");

        TaskInfoMessager.sendMessage(taskInfoMessage);

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("discovery tunnel")
                        .message(msg + result.getFriendlyName())
                        .error(!taskInfoMessage.getSuccessfully())
                        .build());
    }
}
