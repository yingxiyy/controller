package net.flex.dci.otn.controller.implement.common.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TaskNode {
	@JsonProperty("node-ref")
	private String nodeRef;
	
	private StepsWrapper properties;
	
	@JsonProperty("friendly-name")
	private String friendlyName;
	
	private String ip;
}
