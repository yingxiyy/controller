package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.component.site.route.TunnelSiteRouteRetriever;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput.RouteType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/8 17:07
 */

@Component
@Slf4j
public class TunnelSiteRouteHandler extends AbstractBaseHandler {


    private final TunnelSiteRouteRetriever tunnelSiteRouteRetriever;

    public TunnelSiteRouteHandler(
            NetconfTopology netconfTopology, TunnelSiteRouteRetriever tunnelSiteRouteRetriever) {
        super(netconfTopology);
        this.tunnelSiteRouteRetriever = tunnelSiteRouteRetriever;
    }

    @Override
    public GetTunnelSiteRouteOutput getTunnelSiteNodeRoute(GetTunnelSiteRouteInput input) {
        log.debug("start to get the tunnel site route detail info");
        validInput(input);
        String tunnelId = input.getTunnelRef().getValue();
        String siteId = input.getSiteRef().getValue();
        RouteType routeType = input.getRouteType();
        List<RouteInfo> refSiteRouteInfo = tunnelSiteRouteRetriever.retrieveSiteRouteForTunnel(
                tunnelId, siteId, routeType);
        GetTunnelSiteRouteOutputBuilder getTunnelSiteRouteOutputBuilder = new GetTunnelSiteRouteOutputBuilder();
        return getTunnelSiteRouteOutputBuilder.setRouteInfo(refSiteRouteInfo).build();
    }

    private void validInput(GetTunnelSiteRouteInput input) {
        log.debug("valid the get tunnel site route input,the input is :{}", input);
        Uri tunnelRef = input.getTunnelRef();
        NodeId siteRef = input.getSiteRef();
        if (tunnelRef == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tunnel ref should not be null");
        }
        if (siteRef == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tunnel's site should not be null");
        }

    }
}
