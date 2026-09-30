package net.flex.dci.otn.controller.nms.nms.component.route;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.AbstractBaseHandler;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/1 13:28
 */
@Component
@Slf4j
public class LinkRouteHandler extends AbstractBaseHandler {

    private final Map<String, LinkRoute> linkRouteMap = new HashMap<>();

    public LinkRouteHandler(NetconfTopology netconfTopology, List<AbstractLinkRoute> linkRoutes) {
        super(netconfTopology);
        linkRoutes.forEach(route -> {
            linkRouteMap.put(route.linkType(), route);
        });
    }


    /**
     * get route for the link
     *
     * @param input
     * @return
     * @throws Exception
     */
    @Override
    public List<RouteInfo> getRoute(GetRouteInput input)
            throws Exception {
        log.debug("start to get the route for the input:{}", input);
        GetRouteDto getRouteDto = parseGetRouteInput(input);
        return linkRouteMap.get(getRouteDto.LinkType).getRouteById(getRouteDto.id);
    }

    private GetRouteDto parseGetRouteInput(GetRouteInput input) {
        TopologyId topologyRef = input.getTopologyRef();
        LinkId linkRef = input.getLinkRef();
        Uri tunnelRef = input.getTunnelRef();
        String topologyRefName = topologyRef.getValue();
        String linkType = null;
        String refLinkId = null;
        if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && linkRef != null) {
            linkType = Constants.SITE_LINK;
            refLinkId = linkRef.getValue();
        } else if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && tunnelRef != null) {
            linkType = Constants.SITE_TUNNEL;
            refLinkId = tunnelRef.getValue();
        } else if (topologyRefName.equals(Constants.PHY_TOPO_KEY) && linkRef != null) {
            linkType = Constants.PHY_LINK;
            refLinkId = linkRef.getValue();
        } else if (topologyRefName.equals(Constants.OCH_TOPO_KEY) && linkRef != null) {
            linkType = Constants.OCH_LINK;
            refLinkId = linkRef.getValue();
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "can't support retrieve route info on this object");
        }
        return GetRouteDto.builder().id(refLinkId).LinkType(linkType).build();
    }

    @Data
    @Builder
    private static class GetRouteDto implements Serializable {

        private String id;

        private String LinkType;
    }
}
