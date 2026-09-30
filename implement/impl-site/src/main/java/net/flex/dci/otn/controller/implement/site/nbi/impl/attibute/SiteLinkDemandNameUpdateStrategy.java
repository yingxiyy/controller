package net.flex.dci.otn.controller.implement.site.nbi.impl.attibute;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/2/27
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteLinkDemandNameUpdateStrategy implements SiteLinkAttributeUpdateStrategy {

    private final SiteLinkDao siteLinkDao;

    @Override
    public boolean supports(UpdateLinkInput input) {
        return StringUtils.hasText(input.getOrderId());
    }

    @Override
    public void execute(UpdateLinkInput input, TaskInfoMessage taskInfoMessage) {
        String siteLinkId = input.getLinkId();
        String demandSource = input.getOrderId();
        log.info("update link demand source,the site link id:{} and demand source:{}",
                siteLinkId,
                demandSource);
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        updateSiteLinkDemandName(siteLink, demandSource, taskInfoMessage);
    }

    /**
     * update site link demand name
     *
     * @param link
     * @param demandSource
     * @param taskInfoMessage
     */
    private void updateSiteLinkDemandName(Link link, String demandSource,
            TaskInfoMessage taskInfoMessage) {
        log.debug("update link demand source,the site link id:{} and demand name:{}",
                link.getLinkId(),
                demandSource);
        String siteLinkId = link.getLinkId().getValue();
        Site siteLinkPhysical = link.getAugmentation(Link1.class).getSite();
        String friendlyName = siteLinkPhysical.getFriendlyName();
        String oldDemandSource = siteLinkPhysical.getOrderId().get(0);
        taskInfoMessage.setResourceId(siteLinkId);
        String resourceName = String.format("SiteLink %s: Demand Source updated (%s → %s)",
                friendlyName, oldDemandSource, demandSource);
        try {
            siteLinkDao.updateSiteLinkDemandSource(siteLinkId, demandSource);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_DEMAND_SOURCE,
                    resourceName, BLANK, taskInfoMessage);

        } catch (Exception e) {
            log.error("failed update site link demand source,the reason is:{}", e.getMessage(), e);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_DEMAND_SOURCE,
                    resourceName, e.getMessage(), taskInfoMessage);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed update site link demand source,the reason is:" + e.getMessage(), e);
        }
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.updateDemandSource;
    }


}
