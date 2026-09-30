package net.flex.dci.otn.controller.nms.nms.component.site.route.impl;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.component.site.route.SiteRouteInfoConstructor;
import net.flex.dci.otn.controller.nms.nms.component.site.route.SiteRouteRetriever;
import net.flex.dci.otn.controller.nms.nms.component.site.route.TunnelSiteRouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.TunnelSiteRouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.enums.TunnelSiteType;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.RouteCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput.RouteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/16 16:26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelSiteRouteRetrieverImpl implements TunnelSiteRouteRetriever {

    private final TunnelDao tunnelDao;

    private final RouteRetriever routeRetriever;

    private final SiteRouteRetriever siteRouteRetriever;

    private final SiteRouteInfoConstructor siteRouteInfoConstructor;

    private final DciTopologyCacheManager dciTopologyCacheManager;

    @Override
    public List<RouteInfo> retrieveSiteRouteForTunnel(String tunnelId, String siteId,
            RouteType routeType) {
        Tunnel refTunnel = tunnelDao.getTunnelById(tunnelId);
        if (refTunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tunnel id:" + tunnelId + " is not exited");
        }
        TunnelSiteRouteInfoDto tunnelSiteRoute = getAllTunnelSiteRouteInfo(refTunnel);
        String sourceTp = refTunnel.getSourceTp().get(0).getTpRef().getValue();
        String destTp = refTunnel.getDestinationTp().get(0).getTpRef().getValue();
        String sourceSite = PhysicalTpIdNamingRule.getSiteId(sourceTp);
        String destSite = PhysicalTpIdNamingRule.getSiteId(destTp);

        Map<String, SiteRouteDto> siteRouteMap = tunnelSiteRoute.getSiteRoutes().stream().collect(
                HashMap::new, (map, siteRoute) -> map.put(siteRoute.getSiteId(), siteRoute),
                HashMap::putAll);
        if (!siteRouteMap.containsKey(siteId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the site id: %s is not in the tunnel route", siteId));
        }
        SiteRouteDto refSiteRoute = siteRouteMap.get(siteId);
        TunnelSiteType tunnelSiteType = getTunnelSiteType(siteId, sourceSite, destSite);
        List<RouteInfo> refSiteRouteInfo = siteRouteInfoConstructor.constructSiteRouteInfo(
                refSiteRoute, tunnelSiteType, routeType);
        return refSiteRouteInfo;
    }

    /**
     * judge the site is terminal site or not
     *
     * @param siteId
     * @param sourceSite
     * @param destSite
     * @return
     */
    private TunnelSiteType getTunnelSiteType(String siteId, String sourceSite, String destSite) {
        TunnelSiteType tunnelSiteType = TunnelSiteType.TRANSIT_SITE;
        if (siteId.equals(sourceSite)) {
            tunnelSiteType = TunnelSiteType.SOURCE_SITE;
        } else if (siteId.equals(destSite)) {
            tunnelSiteType = TunnelSiteType.DEST_SITE;
        }
        return tunnelSiteType;
    }

    private TunnelSiteRouteInfoDto getAllTunnelSiteRouteInfo(Tunnel refTunnel) {
        Gson gson = new Gson();
        String tunnelId = refTunnel.getTunnelId().getValue();
        RouteCache routeCache = dciTopologyCacheManager.getRoute(tunnelId);
        TunnelSiteRouteInfoDto tunnelSiteRouteInfoDto = null;
        if (routeCache != null) {
            tunnelSiteRouteInfoDto = gson.fromJson(routeCache.getRoute(),
                    TunnelSiteRouteInfoDto.class);
        } else {
            log.debug("get tunnel site route info ");
            List<Route> routes = refTunnel.getExplictRoute()
                    .getRoute();

            //assumption: there only have one route
            Route route = routes.get(0);
            String sourceSite = PhysicalTpIdNamingRule.getSiteId(
                    refTunnel.getSourceTp().get(0).getTpRef().getValue());
            String destSite = PhysicalTpIdNamingRule.getSiteId(
                    refTunnel.getDestinationTp().get(0).getTpRef().getValue());
            RouteSequenceDto routeSequence = routeRetriever.retrieverRouteSequence(
                    route, sourceSite, destSite);
            tunnelSiteRouteInfoDto = siteRouteRetriever.retrieveTunnelSiteRoute(
                    tunnelId, routeSequence);
            dciTopologyCacheManager.refreshRoute(tunnelId, gson.toJson(tunnelSiteRouteInfoDto));
        }
        return tunnelSiteRouteInfoDto;
    }


}
