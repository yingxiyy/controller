package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class RouteSegment implements Serializable {

    private String sourceTp;

    private String linkId;

    private String destTp;
}
