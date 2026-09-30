package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/7 15:47
 */
@Data
@Builder
public class ThumbnailRouteDto implements Serializable {

    private ThumbnailRouteDetailDto primary;

    private ThumbnailRouteDetailDto secondary;

    private ThumbnailRouteDetailDto tertiary;
}
