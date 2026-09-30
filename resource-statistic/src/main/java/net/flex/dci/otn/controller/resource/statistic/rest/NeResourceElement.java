package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 * 2025/10/26
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class NeResourceElement implements Serializable {

    private String sn;

    private String pn;

    private String vendor;

    private String name;

    private String type;

    private String hwVersion;

    private String swVersion;

    private String fwVersion;

    private String slotNumber;

    private String mfgDate;
}
