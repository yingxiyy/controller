package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.design;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;

/**
 * @version 1.0
 * @date 7/15/2025 4:57 PM
 */
@Data
@Builder
public class DesignRouteObject implements Serializable {

    private String id;

    private RetrieveType type;
}
