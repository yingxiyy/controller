package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ReachableSiteDetail implements Serializable {

    private String siteId;

    private List<ExternalEdge> externalEdges;

}
