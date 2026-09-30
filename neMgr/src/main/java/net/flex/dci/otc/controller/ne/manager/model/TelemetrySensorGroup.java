package net.flex.dci.otc.controller.ne.manager.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 6/11/2025 11:22 AM
 */
@Data
public class TelemetrySensorGroup implements Serializable {

    private List<SensorGroupInfo> telemetrySensorGroup;

}
