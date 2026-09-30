package net.flex.dci.otn.controller.implement.site.nbi.impl.attibute;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dto.batch.RackFriendlyName;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
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
public class SiteLinkFriendlyNameUpdateStrategy implements SiteLinkAttributeUpdateStrategy {

    private final SiteLinkDao siteLinkDao;

    private final SiteNodeDao siteNodeDao;

    private final RackDao rackDao;

    @Override
    public boolean supports(UpdateLinkInput input) {
        return StringUtils.hasText(input.getFriendlyName());
    }

    @Override
    public void execute(UpdateLinkInput input, TaskInfoMessage taskInfoMessage) {
        String siteLinkId = input.getLinkId();
        log.info("update siteLink:{} friendly name", siteLinkId);
        String newFriendlyName = input.getFriendlyName();
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        if (!StringUtils.hasText(newFriendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "only support change friendlyName, and length of friendlyName must be large than 0");
        }
        checkDuplicate(newFriendlyName);
        updateSiteLinkFriendlyName(siteLink, newFriendlyName, taskInfoMessage);
    }

    private void updateSiteLinkFriendlyName(Link siteLink, String newFriendlyName,
            TaskInfoMessage taskInfoMessage) {
        log.debug("update site link friendly name the site link is:{} new friendly name:{}",
                siteLink.getLinkId(), newFriendlyName);
        String oldFriendlyName = siteLink.getAugmentation(Link1.class).getSite()
                .getFriendlyName();
        String resourceName = String.format("SiteLink %s: friendly name update to %s",
                oldFriendlyName, newFriendlyName);
        try {
            String siteLinkId = siteLink.getLinkId().getValue();
            taskInfoMessage.setResourceId(siteLinkId);
            siteLinkDao.updateLinkFriendlyName(siteLinkId, newFriendlyName);
            updateRelatedRackFriendlyNames(siteLinkId, newFriendlyName,
                    getRelatedSiteNodeIds(siteLink));
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_FRIEND_NAME,
                    resourceName, BLANK, taskInfoMessage);
        } catch (Exception e) {
            log.error("failed to update site link physical name,the reason is:{}", e.getMessage(),
                    e);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_FRIEND_NAME,
                    resourceName, e.getMessage(), taskInfoMessage);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "update site link friendly name failed " + e.getMessage(), e);
        }
    }

    void updateRelatedRackFriendlyNames(String siteLinkId, String newFriendlyName,
            Set<String> relatedSiteNodeIds) {
        log.debug("update the related rack friendly names like rack");
        List<Node> siteNodes = siteNodeDao.listAllNodeByIds(new ArrayList<>(relatedSiteNodeIds));
        Map<String, String> siteRefRackIdMap = new HashMap<>();
        for (Node site : siteNodes) {
            String siteId = site.getNodeId().getValue();
            Site sitePhysical = site.getAugmentation(Node1.class).getSite();
            sitePhysical.getSupportingRack().stream()
                    .map(rack -> rack.getRackId().getValue())
                    .filter(rackId -> rackId.contains(siteLinkId)).findAny()
                    .ifPresent(refRack -> siteRefRackIdMap.put(siteId, refRack));
        }

        List<RackFriendlyName> rackFriendlyNames = new ArrayList<>();
        //update the site rack
        for (Map.Entry<String, String> entry : siteRefRackIdMap.entrySet()) {
            String siteNodeId = entry.getKey();
            String rackId = entry.getValue();
            RackFriendlyName rackFriendlyName = RackFriendlyName.builder()
                    .friendlyName(newFriendlyName)
                    .rackId(rackId)
                    .siteId(siteNodeId)
                    .build();
            rackFriendlyNames.add(rackFriendlyName);
        }
        rackDao.bulkUpdateRackFriendlyName(rackFriendlyNames);
//        int updatedRackCount = 0;
//        for (String siteNodeId : relatedSiteNodeIds) {
//            Node siteNode = siteNodeDao.getSiteNodeById(siteNodeId);
//            if (siteNode == null) {
//                log.warn("cannot find related siteNode:{} when renaming siteLink:{}", siteNodeId,
//                        siteLinkId);
//                continue;
//            }
//            Node1 nodeAttribute = siteNode.getAugmentation(Node1.class);
//            Site site = nodeAttribute == null ? null : nodeAttribute.getSite();
//            List<SupportingRack> racks = site == null ? null : site.getSupportingRack();
//            if (racks == null) {
//                continue;
//            }
//
//            for (SupportingRack rack : racks) {
//                if (rack.getRackId() != null && Arrays.asList(SiteRackIdNamingRule.extractKey(
//                        rack.getRackId().getValue()).split(",")).contains(siteLinkId)) {
//                    rackDao.updateRack2Site(siteNodeId, new SupportingRackBuilder(rack)
//                            .setFriendlyName(newFriendlyName)
//                            .build());
//                    updatedRackCount++;
//                    break;
//                }
//            }
//        }
//        if (updatedRackCount == 0) {
//            log.warn("no related rack found when updating siteLink:{} friendly name", siteLinkId);
//        } else {
//            log.info("updated {} rack friendly names for siteLink:{}", updatedRackCount,
//                    siteLinkId);
//        }
    }


    private Set<String> getRelatedSiteNodeIds(Link siteLink) {
        List<String> supportingLinkIds = siteLink.getSupportingLink().stream()
                .map(link -> link.getLinkRef().getValue())
                .collect(Collectors.toList());
        Set<String> siteNodeIds = new HashSet<>();
        for (String supportingLinkId : supportingLinkIds) {
            String siteIdA = PhysicalLinkIdNamingRule.getSiteAId(supportingLinkId);
            String siteIdZ = PhysicalLinkIdNamingRule.getSiteZId(supportingLinkId);
            siteNodeIds.add(siteIdZ);
            siteNodeIds.add(siteIdA);
        }
        return siteNodeIds;
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.updateFriendlyName;
    }


    private void checkDuplicate(String friendlyName) throws CommonException {
        if (siteLinkDao.existsLinkFriendlyName(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the friendlyName is duplicated with existed one " + friendlyName);
        }
    }


}
