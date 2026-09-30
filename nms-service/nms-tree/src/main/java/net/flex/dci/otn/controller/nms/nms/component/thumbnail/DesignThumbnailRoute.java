package net.flex.dci.otn.controller.nms.nms.component.thumbnail;

import java.util.List;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;

/**
 * @version 1.0
 * @date 7/15/2025 4:30 PM
 */
public interface DesignThumbnailRoute {

    ThumbnailRouteDto getDesignThumbnailSequence(List<String> primary,
            List<String> secondary, List<String> tertiary);
}
