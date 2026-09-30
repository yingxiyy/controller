package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseInfo implements Serializable {


    private String neName;

    private String neVendor;


    private String siteName;

    private String neIp;

    private String neType;

    private String neSubType;

    private String subnet;
}
