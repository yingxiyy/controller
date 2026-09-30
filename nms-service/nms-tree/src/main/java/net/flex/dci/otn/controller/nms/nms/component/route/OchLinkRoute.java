package net.flex.dci.otn.controller.nms.nms.component.route;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/23 14:03
 */
@Slf4j
@Component
public class OchLinkRoute extends AbstractLinkRoute {


    public OchLinkRoute(NetconfTopology netconfTopology, RouteRetriever routeRetriever) {
        super(netconfTopology, routeRetriever);
    }


    @Override
    public List<RouteInfo> getRouteById(String id) throws CommonException {
        log.debug("get och link route by id :{}", id);
        Link ochLink = netconfTopology.getOchLink(id);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("there have no route for the och link :%s", id));
        }
        RouteInfoDto routeInfoDto = getRouteInfoDto(ochLink);
        List<RouteInfo> routes = routeRetriever.retrieverRouteInfo(routeInfoDto);
        return routes;
    }

    private RouteInfoDto getRouteInfoDto(Link ochLink) {
        Link1 siteLinkAugmentation = ochLink.getAugmentation(Link1.class);
        String source = PhysicalNodeIdNamingRule.getSiteId(
                ochLink.getSource().getSourceNode().getValue());
        String destination = PhysicalNodeIdNamingRule.getSiteId(
                ochLink.getDestination().getDestNode().getValue());
        return RouteInfoDto.builder()
                .source(source)
                .destination(destination)
                .connectionId(ochLink.getLinkId().getValue())
                .routes(getRealRoute(siteLinkAugmentation.getOch().getExplictRoute().getRoute()))
                .build();
    }

    @Override
    public String linkType() {
        return Constants.OCH_LINK;
    }
}
