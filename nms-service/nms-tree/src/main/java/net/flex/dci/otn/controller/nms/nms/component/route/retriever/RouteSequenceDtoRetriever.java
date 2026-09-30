package net.flex.dci.otn.controller.nms.nms.component.route.retriever;

import net.flex.dci.otn.controller.nms.nms.dto.RouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;

/**
 * @version 1.0
 * @date 2022/12/2 16:03
 */
public interface RouteSequenceDtoRetriever {

    RouteDetailDto retrieveRouteDetail(RouteSequenceDto routeSequenceDto);
}
