package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/8/1
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtdrTaskInfoResult implements Serializable {

    private String linkId;

    private String otdrResultIdOnNe;

    private String monitorPort;

    private long otdrDbResultId;

}
