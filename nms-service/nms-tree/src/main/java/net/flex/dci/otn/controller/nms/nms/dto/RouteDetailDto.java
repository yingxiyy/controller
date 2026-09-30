package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/6/29 17:22
 */
@Data
@Builder
public class RouteDetailDto implements Serializable {

    private RouteDto primary;
    private RouteDto secondary;
    private List<RouteDto> tertiary;


}
