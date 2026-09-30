package net.flex.dci.otn.controller.resource.statistic.rest.equipment;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.flex.dci.otn.controller.resource.statistic.rest.BaseInfo;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TransceiverInfo extends BaseInfo implements Serializable {


    private String transceiverId;

    private String transceiverName;


    private String shelf;

    private String slot;

    private String serialNo;

    private String vendorName;

    private String hwVersion;

    private String swVersion;

    private boolean removable;

    private String partNo;

    private String fwVersion;

    private String description;

    private String connectorType;
    private String formFactor;
    private String ethernetPmd;
    private String ethernetPmdPreconf;

    private String mfgDate;

    private Double minTemperature;
    private Double maxTemperature;

    private Double minInputPower;
    private Double maxInputPower;
    private Double minOutputPower;
    private Double maxOutputPower;
    private Double minLaneInputPower;
    private Double maxLaneInputPower;
    private Double minLaneOutputPower;
    private Double maxLaneOutputPower;

    // 电压电流
    private Double minInputVoltage;
    private Double maxInputVoltage;
    private Double minInputCurrent;
    private Double maxInputCurrent;
    private Double allocatedPower;

    // 激光偏置电流
    private Double minLaserBiasCurrent;
    private Double maxLaserBiasCurrent;
    private Double minLaneLaserBiasCurrent;
    private Double maxLaneLaserBiasCurrent;
}
