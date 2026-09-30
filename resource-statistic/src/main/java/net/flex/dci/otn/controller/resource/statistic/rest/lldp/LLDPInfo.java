package net.flex.dci.otn.controller.resource.statistic.rest.lldp;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import net.flex.dci.otn.controller.resource.statistic.rest.BaseInfo;

/**
 * @version 1.0
 * @date 10/31/2025 4:40 PM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
public class LLDPInfo extends BaseInfo implements Serializable {

//    private String tpName;
//
//    private String tpType;
//
//    private String neighborId;
//
//    private Long age;
//
//    private String lastUpdate;
//
//    private String neighborPortId;
//
//    private String neighborPortIdType;
//
//    private String neighborPortDescription;
//
//    private String managementAddress;
//
//    private String managementAddressType;
//
//    private String systemName;
//
//    private String systemDescription;
//
//    private String chassisId;
//
//    private String chassisIdType;
//
//    private String capability;

    private String tunnelId;

    private String subnet;

    private String tunnelName;

    private String sourceSite;

    private String sourceNe;

    private String sourceLinePort;

    private String sourceClientPort;


    private String aTransmissionRemoteChassis;
    private String aTransmissionRemotePort;
    private String aTransmissionManagerAddress;
    private String aTransmissionSystemName;

    private Long sourceNeighborEstablishTime;

    private String destinationSite;

    private String destinationNe;

    private String destinationLinePort;

    private String destinationClientPort;


    private String zTransmissionRemoteChassis;
    private String zTransmissionRemotePort;
    private String zTransmissionManagerAddress;
    private String zTransmissionSystemName;


    private Long destinationNeighborEstablishTime;
}
