package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.ExcelIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @date 10/31/2025 3:02 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NeDeviceCsv implements Serializable {

    private String neId;

    @ExcelIgnore
    private String siteId;

    private String siteName;

//    private String location;
//
//    private String city;
//
//    private String country;
//
//    private String campus;

    private String vendor;


    private String deviceType;

    private String deviceSubType;


    private String vendorType;

    private String swVersion;

    private String subnet;

    private String neName;

    private String ipAddress;

    private Integer port;

    private String neAccount = "";

    private String nePassword = "";


    private String northApiVersion;

    private String createTime;

    private String relativeSiteLink;

    private String relativeServices;
}
