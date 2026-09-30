package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.mongo.enums.NeSubType;

/**
 * 2026/4/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ExternalEdge implements Serializable {

    private String sourceSiteId;

    private String destSiteId;

    private String siteLinkId;

    private String sourceNeId;

    private String destNeId;

    private String sourceTpId;

    private String destTpId;

    private NeSubType sourceNeSubType;

    private NeSubType destNeSubType;
}
