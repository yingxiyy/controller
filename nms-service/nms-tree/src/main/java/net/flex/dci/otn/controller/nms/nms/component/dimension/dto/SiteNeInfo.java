package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
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
public class SiteNeInfo implements Serializable {

    private Set<String> neIds;

    private List<InternalEdge> internalEdges;
}
