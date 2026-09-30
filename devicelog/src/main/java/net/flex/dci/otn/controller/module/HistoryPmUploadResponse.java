package net.flex.dci.otn.controller.module;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@Data
public class HistoryPmUploadResponse implements Serializable {
	@JsonProperty("return-message")
	String returnMessage;
	
	@JsonProperty("return-code")
	String returnCode;
}
