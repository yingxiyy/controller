package net.flex.dci.otn.controller.nms.nms.dto.link;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.mongo.enums.NeSubType;

/**
 * 2026/5/26
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SiteLinkGeneralInfo implements Serializable {

    private String siteLinkId;

    private String siteLinkName;

    private String sourceNeId;

    private NeSubType sourceNeSubType;

    private String destinationNeId;

    private NeSubType destinationNeSubType;

    private String sourceSiteId;

    private String destinationSiteId;

    private String subnetId;

    private String subnetName;

}