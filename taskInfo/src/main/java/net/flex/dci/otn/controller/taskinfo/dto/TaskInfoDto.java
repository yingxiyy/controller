package net.flex.dci.otn.controller.taskinfo.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otc.common.model.TaskInfoMessage;

/**
 * @version 1.0
 * @date 2022/5/2 9:36
 */
@Data
@Builder
@AllArgsConstructor
public class TaskInfoDto implements Serializable {

    private Long id;
    private String resourceId;
    private String resourceName;
    private TaskInfoMessage.ResourceType resourceType;
    //    @Column(nullable = false)
    private String who;
    private Date actionTime;
    private Date endTime;
    private TaskInfoMessage.ActionType actionType;
    private Boolean successfully;
    private String errorReason;
    private Boolean hasDetail;
    private Object detail;
    private Long groupId;
    private Boolean root;
    private String objectId;
    private String objectType;
    private String scanResultId;

    @Tolerate
    public TaskInfoDto() {

    }
    @Override
    public String toString() {
        String detailStr = detail == null ? "" : detail.toString();

        return "TaskInfoDto{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", root=" + root +
                ", who='" + who + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", resourceType=" + resourceType +
                ", objectId='" + objectId + '\'' +
                ", objectType='" + objectType + '\'' +
                ", actionType=" + actionType +
                ", scanResultId='" + scanResultId + '\'' +
                ", actionTime=" + actionTime +
                ", endTime=" + endTime +
                ", successfully=" + successfully +
                ", errorReason='" + errorReason + '\'' +
                ", hasDetail=" + hasDetail +
                ", detail=" + (detailStr.length() > 200 ? detailStr.substring(0, 200) + "..." : detailStr) +
                '}';
    }
}
