package net.flex.dci.otn.controller.implement.common.recorder;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import net.flex.dci.otn.controller.implement.common.impl.ConfigNeSequence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *     {
 *       "nodes": [
 *         ....
 *       ],
 *       "progress": 66,
 *       "result": "failure",
 *       "start": 1645079430885,
 *       "end": 1645079438239
 *       "link-id":
 *       "friendly-name":
 *     }
 *
 *   lifecycle中记录的详细信息 （content的内容)
 *
 */
@Data
public class TaskRecord {
    @SerializedName("nodes")
    private List<StepRecord> stepRecordList;

    private int progress;
    private String result;

    @SerializedName("start")
    private Long startTime;

    @SerializedName("end")
    private Long endTime;

    @SerializedName("link-id")
    private String linkId;

    @SerializedName("friendly-name")
    private String friendlyName ;

    public TaskRecord(String linkId, String friendlyName) {
        this.progress = 0;
        this.result = "doing";
        this.startTime = System.currentTimeMillis();
        this.endTime = null;
        this.linkId = linkId;
        this.friendlyName = friendlyName;
        this.stepRecordList = new CopyOnWriteArrayList<>();
    }

    public void setStepRecord(StepRecord stepRecord) {
        boolean found = false;
        for (StepRecord existedStepRecord : stepRecordList) {
            if (existedStepRecord.getNodeId().equals(stepRecord.getNodeId())) {
                existedStepRecord.setProperties(stepRecord.getProperties());
                found = true;
                break;
            }
        }
        if (!found) {
            stepRecordList.add(stepRecord);
        }
        setProcess();
    }

    private void setProcess() {
        long success = 0;
        int total = 0;

        for (StepRecord existedStepRecord : stepRecordList) {
            success += existedStepRecord.getProperties().propertyList.stream().filter(e -> e.value.equals(ConfigNeSequence.STATUS_SUCCESS)).count();
            total += existedStepRecord.getProperties().getPropertyList().size();
        }
        progress = (int)(((float)success/total) * 100);
    }

    public void setResult(String result) {
        this.result = result;
        this.progress = 100;
        this.endTime = System.currentTimeMillis();
    }
}
