package net.flex.dci.otn.controller.nms.nms.component.route;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/23 14:03
 */
@Slf4j
@Component
public class TunnelRoute extends AbstractLinkRoute {


    public TunnelRoute(NetconfTopology netconfTopology, RouteRetriever routeRetriever) {
        super(netconfTopology, routeRetriever);
    }

    @Override
    public List<RouteInfo> getRouteById(String id) throws CommonException {
        log.debug("get tunnel route by id :{}", id);
        Tunnel tunnel = netconfTopology.getTunnel(id);
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "there have no route for the tunnel");
        }
        RouteInfoDto tunnelRouteInfoDto = getRouteInfoDto(tunnel);
        List<RouteInfo> routes = routeRetriever.retrieverRouteInfo(tunnelRouteInfoDto);
        return routes;
    }

    private RouteInfoDto getRouteInfoDto(Tunnel tunnel) {
        String sourceSite = PhysicalTpIdNamingRule.getSiteId(
                tunnel.getSourceTp().get(0).getTpRef().getValue());
        String destSite = PhysicalTpIdNamingRule.getSiteId(
                tunnel.getDestinationTp().get(0).getTpRef().getValue());
        return RouteInfoDto.builder()
                .source(sourceSite)
                .destination(destSite)
                .connectionId(tunnel.getTunnelId().getValue())
                .routes(getRealRoute(tunnel.getExplictRoute().getRoute()))
                .build();
    }


    @Override
    public String linkType() {
        return Constants.SITE_TUNNEL;
    }


}
