package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
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
public class ReachabilityResult implements Serializable {

    private String sourceSiteId;

    private String destSiteId;

    private boolean reachable;


    private Set<String> reachableSites;

    private Set<String> reachableNes;

    private List<InternalEdge> internalEdges;

    private List<ExternalEdge> externalEdges;

    private Set<String> twoDimensionSite;

}
