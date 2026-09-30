package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 8/20/2025 3:12 PM
 */
@Data
@Builder
public class RouteSegment implements Serializable {

    private String sourceTp;

    private String linkId;

    private String destTp;
}
