package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class InternalEdge implements Serializable {

    private String internalLinkId;

    private String siteId;

    private String sourceNeId;

    private String destNeId;

    private String sourceTpId;

    private String destTpId;

    private boolean boundaryPort = false;
}
