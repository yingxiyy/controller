package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.ExcelIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 11/7/2025 1:48 PM
 */
@Data
@Builder
@AllArgsConstructor
public class CardCsv implements Serializable {


    private String siteName;

    private String subnet;

    @ExcelIgnore
    private String neId;

    private String neName;

    private String neVendorName;

    private String neType;

    private String neSubType;

    private String neIp;

    @ExcelIgnore
    private String equipmentId;

    private String equipmentName;

    private String equipmentType;

    private String vendorName;

//    private String partName;

    private String assetManagementCode;

    private String PN;

    private String SN;

    private String hwVersion;

    private String swVersion;

    private String fwVersion;

    private String mfgDate;

    private String description;

    @ExcelIgnore
    private String siteId;


}
