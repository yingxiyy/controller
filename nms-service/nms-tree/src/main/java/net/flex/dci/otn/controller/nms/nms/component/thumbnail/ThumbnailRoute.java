package net.flex.dci.otn.controller.nms.nms.component.thumbnail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.thumbnail.link.AbstractLinkThumbnailRoute;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/6 17:02
 */
@Component
@Slf4j
public class ThumbnailRoute implements LinkThumbnailRoute {

    private final Map<String, LinkThumbnailRoute> routeMap = new HashMap<>();


    public ThumbnailRoute(List<AbstractLinkThumbnailRoute> thumbnailRoutes) {
        thumbnailRoutes.forEach(
                linkThumbnailRoute -> routeMap.put(linkThumbnailRoute.getLINK_TYPE(),
                        linkThumbnailRoute));
    }

    @Override
    public ThumbnailRouteDto getThumbnailSequence(String id) {
        log.debug("get thumbnail sequence for the link id is:{}", id);
        String linkType = getLinkTypeById(id);
        return routeMap.getOrDefault(linkType, new DefaultLinkThumbnailRoute())
                .getThumbnailSequence(id);
    }

    /**
     * get the link type
     *
     * @param id
     * @return
     */
    private String getLinkTypeById(String id) {
        String linkType = null;
        if (TunnelIdNamingRule.isTunnelId(id)) {
            linkType = Constants.SITE_TUNNEL;
        } else if (SiteLinkIdNamingRule.isSiteLink(id)) {
            linkType = Constants.SITE_LINK;
        } else if (OchLinkIdNamingRule.isOchLink(id)) {
            linkType = Constants.OCH_LINK;
        }
        return linkType;
    }


}
