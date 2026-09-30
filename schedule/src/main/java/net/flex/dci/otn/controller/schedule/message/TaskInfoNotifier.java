package net.flex.dci.otn.controller.schedule.message;

import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.getDateStr;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otn.controller.schedule.enums.ExecuteResult;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskInfoNotifier {

    // send message to task center TaskInfoMessage(
// id=null, resourceId=null, resourceType=CtrlDB, resourceName=null, who=system, objectId=null, objectType=null,
// actionTime=1765144381317, scanResultId=null, endTime=1765144381317, actionType=ctrlDBBackup,
// successfully=true, errorReason=Success, detail=, groupId=null, root=true)
    public void sendCtrlDbBackupStart(long actionTime, MoSchedule schedule) {
        try {
            TaskInfoMessage msg = new TaskInfoMessage(
                    schedule.getOperator(),
                    TaskInfoMessage.ResourceType.CtrlDB,
                    TaskInfoMessage.ActionType.ctrlDBBackup,
                    "");

            msg.setResourceId("ControllerDbBackup_" + actionTime);
            msg.setResourceName(
                    TaskInfoMessage.ResourceType.CtrlDB.name() + getDateStr(actionTime));
            msg.setErrorReason(
                    schedule.getResultInfo());
            msg.setSuccessfully(false);
            msg.setActionTime(actionTime);
//            msg.setEndTime(System.currentTimeMillis());
            msg.setRoot(true);

            log.debug("send message to task center {}", msg);
            TaskInfoMessager.sendMessage(msg);
        } catch (Exception e) {
            log.error("failed to send task info message to task center", e);
        }
    }

    public void sendCtrlDbBackupFinished(long actionTime, MoSchedule schedule) {
        try {
            TaskInfoMessage msg = new TaskInfoMessage(
                    schedule.getOperator(),
                    TaskInfoMessage.ResourceType.CtrlDB,
                    TaskInfoMessage.ActionType.ctrlDBBackup,
                    "");

            msg.setResourceId("ControllerDbBackup_" + actionTime);
            msg.setResourceName(
                    TaskInfoMessage.ResourceType.CtrlDB.name() + getDateStr(actionTime));
            msg.setErrorReason(
                    schedule.getResultInfo());
            msg.setSuccessfully(
                    schedule.getResult().equalsIgnoreCase(ExecuteResult.Success.name()));
            msg.setActionTime(actionTime);
            msg.setEndTime(System.currentTimeMillis());
            msg.setRoot(true);

            log.debug("send message to task center {}", msg);
            TaskInfoMessager.sendMessage(msg);
        } catch (Exception e) {
            log.error("failed to send task info message to task center", e);
        }
    }
}
