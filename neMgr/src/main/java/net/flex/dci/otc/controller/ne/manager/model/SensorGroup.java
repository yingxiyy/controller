package net.flex.dci.otc.controller.ne.manager.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 6/11/2025 11:24 AM
 */
@Data
public class SensorGroup implements Serializable {

    private String sensorGroupName;

    private List<String> sensorGroupPath;

    private Long sampleInterval = 10000L;

    private Long heartbeatInterval = 60000L;

    private Boolean suppressRedundant = true;

}
