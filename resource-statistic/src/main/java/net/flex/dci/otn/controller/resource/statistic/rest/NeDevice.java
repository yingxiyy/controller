package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 2025/10/26
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class NeDevice implements Serializable {

    private String siteId;
    private String siteName;
    private String location;

    private String city;

    private String country;

    private String campus;

    private String vendor;


    private String deviceType;

    private String neSubType;

    private String ipAddress;

    private String userAccount;

    private String password;

    private Integer port;

    private String neId;

    private String name;

    private String vendorType;

    private String swVersion;


    private String northApiVersion;

    private String createTime;

    private String network;

    private List<NeResourceElement> elements;

    private String refSiteLink;

    private String serviceConnect;
}
