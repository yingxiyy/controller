package net.flex.dci.otn.controller.implement.common.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;

import java.util.List;

@Data
public class ResultLinks {
	@JsonProperty("link-id")
	private String linkId;
	private int progress;
	private ExecuteResult result;
	@JsonProperty("friendly-name")
	private String friendlyName;
	private long start;
	private long end;
//	private List<TaskNode> nodes;
	private List<StepRecord> nodes;
}
