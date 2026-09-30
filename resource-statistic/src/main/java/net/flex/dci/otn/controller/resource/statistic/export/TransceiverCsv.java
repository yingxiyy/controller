package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.ExcelIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光模块导出 CSV 数据模型
 *
 * @author musa
 * @version 1.0
 * @date 2026/4/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransceiverCsv implements Serializable {

    @ExcelIgnore
    private String neId;

    private String neName;

    private String neVendorName;

    private String neType;

    private String neSubType;

    private String neIp;


    @ExcelIgnore
    private String subnetId;

    private String subnet;

    @ExcelIgnore
    private String siteId;

    private String siteName;

    @ExcelIgnore
    private String transceiverId;

    private String transceiverName;

    private String shelf;

    private String slot;

    private String serialNo;

    private String vendorName;

    private String hwVersion;

    private String swVersion;

    private String fwVersion;

    private String partNo;

    private String description;

    private String connectorType;

    private String formFactor;

    private String ethernetPmd;

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
