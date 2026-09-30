package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.ExcelIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/11/2
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
public class LLDPCsv implements Serializable {

//    private String neId;
//
//    private String neName;
//
//    private String neIp;
//
//    private String neVendor;
//
//    @ExcelIgnore
//    private String siteId;
//
//    private String siteName;
//
//    private String tpName;
//
//    @ExcelIgnore
//    private String tpId;
//
//    private String tpType;
//
//    //lldp info
//
//    private String systemName;
//
//    private String systemDescription;
//
//    private String chassisId;
//
//    private String chassisIdType;
//
//    private String neighborId;
//
//    private Long age;
//
//    private Long lastUpdate;
//
//
//    private String neighborPortId;
//
//    private String neighborPortIdType;
//
//    private String neighborPortDescription;
//
//    private String managementAddressType;
//
//    private String managementAddress;
//
//    private String capability;

    @ExcelIgnore
    private String tunnelId;

    private String subnet;

    private String tunnelName;

    private String sourceSite;

    private String sourceNe;

    private String sourceLinePort;

    private String sourceClientPort;


    private String sourceTransmissionRemoteChassis;
    private String sourceTransmissionRemotePort;
    private String sourceTransmissionManagerAddress;
    private String sourceTransmissionSystemName;

    private String sourceNeighborEstablishTime;


    private String destinationSite;

    private String destinationNe;

    private String destinationLinePort;

    private String destinationClientPort;


    private String destinationTransmissionRemoteChassis;
    private String destinationTransmissionRemotePort;
    private String destinationTransmissionManagerAddress;
    private String destinationTransmissionSystemName;

    private String destinationNeighborEstablishTime;
}
