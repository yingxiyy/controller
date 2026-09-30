package net.flex.dci.otc.controller.status.core.evaluate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.alarm.detail.link.ViewLinkAlarmStateChanger;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinksAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/23/2023 4:14 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewLinkAlarmStateEvaluate implements StateEvaluate {

    private final ViewLinkAlarmStateChanger viewLinkAlarmStateChanger;

    private final ViewLinkDao viewLinkDao;

    private final PhyLinkDao phyLinkDao;

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;


    @Override
    public void evaluateAlarmState(List<String> linkIds, LinkType linkType) {
        log.debug("start to evaluate the link ids:{} relative view link alarm state", linkIds);
        // De-duplicate input ids first to avoid repeated DB lookups in the same batch.
        List<Link> viewLinks = viewLinkDao.getViewLinkBySupportingLinks(linkIds);
        if (!viewLinks.isEmpty()) {
            List<ViewLinkAlarmState> viewLinkAlarmStates = calculateViewLinksAlarmState(
                    new ArrayList<>(viewLinks.stream()
                            .collect(Collectors.toMap(link -> link.getLinkId().getValue(),
                                    link -> link,
                                    (existing, replacement) -> existing))
                            .values()),
                    linkType);
            viewLinkAlarmStateChanger.changeState(
                    ViewLinksAlarmState.builder().viewLinkAlarmStates(viewLinkAlarmStates).build());
        }
    }

    @Override
    public List<ViewLinkAlarmState> calculateViewLinksAlarmState(List<Link> viewLinks,
            LinkType linkType) {
        log.debug("start to calculate view link alarm state , current relative link type is:{}",
                linkType);
        Map<String, Link> linkCache = new HashMap<>();
        List<ViewLinkAlarmState> viewLinkAlarmStates = viewLinks.stream()
                .map(viewLink -> calculateViewLinkAlarmState(viewLink, linkType, linkCache))
                .collect(Collectors.toList());

        return viewLinkAlarmStates;
    }

    private ViewLinkAlarmState calculateViewLinkAlarmState(Link viewLink, LinkType linkType,
            Map<String, Link> linkCache) {
        log.debug("calculate the view link alarm state,the view link is:{}",
                viewLink);
        String viewLinkId = viewLink.getLinkId().getValue();
        List<String> originalSupportingLinks = viewLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .collect(Collectors.toList());
        List<Link> validSupportingLinks = originalSupportingLinks.stream()
                .map(linkId -> getLinkByLinkId(linkId, linkType, linkCache))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (validSupportingLinks.size() != originalSupportingLinks.size()) {
            //update the view link general
            if (!validSupportingLinks.isEmpty()) {
                updateViewLinkGeneral(viewLink, validSupportingLinks);
            } else {
                viewLinkDao.deleteViewLink(viewLinkId);
            }
        }
        List<AlarmSeverity> alarmSeverities = validSupportingLinks.stream()
                .map(link -> this.getAlarmSeverity(link, linkType)).collect(
                        Collectors.toList());
        AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmStateByList(alarmSeverities);
        return ViewLinkAlarmState.builder().viewLinkId(viewLinkId).alarmSeverity(alarmSeverity)
                .build();
    }

    /**
     * update view link general
     *
     * @param viewLink
     * @param validSupportingLinks
     */
    private void updateViewLinkGeneral(Link viewLink, List<Link> validSupportingLinks) {
        log.debug("update viewLink id:{} general", viewLink.getLinkId());
        List<SupportingLink> supportingLinks = validSupportingLinks.stream()
                .map(LinkAttributes::getLinkId)
                .map(linkId -> new SupportingLinkBuilder().setLinkRef(linkId).build())
                .collect(
                        Collectors.toList());

        int size = supportingLinks.size();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View viewLinkPhysical = viewLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class)
                .getView();
        LinkBuilder linkBuilder = new LinkBuilder(viewLink);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder viewBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder(
                viewLinkPhysical);
        viewBuilder.setBundleNumber(size);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setView(viewBuilder.build());
        linkBuilder.setSupportingLink(supportingLinks);
        linkBuilder.addAugmentation(
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class,
                link1Builder.build());
        viewLinkDao.saveViewLink(linkBuilder.build());
    }


    private Link getLinkByLinkId(String linkId, LinkType linkType, Map<String, Link> linkCache) {
        if (isDummyLink(linkId, linkType)) {
            return null;
        }
        Link cached = linkCache.get(linkId);
        if (cached != null) {
            return cached;
        }
        log.debug("get link by linkId:{} linkType:{}", linkId, linkType);
        Link link = null;
        switch (linkType) {
            case OCH_LINK:
                link = ochLinkDao.getOchLinkByLinkId(linkId);
                break;
            case PHYSICAL_LINK:
                link = phyLinkDao.getPhyLinkById(linkId);
                break;
            case SITE_LINK:
                link = siteLinkDao.getSiteLinkById(linkId);
                break;
        }
        if (link != null) {
            linkCache.put(linkId, link);
        }
        return link;
    }

    private boolean isDummyLink(String linkId, LinkType linkType) {
        if (linkId == null) {
            return true;
        }
        if (linkType.equals(LinkType.OCH_LINK) && linkId.contains("EXP33")) {
            return true;
        }
        return false;
    }

    private AlarmSeverity getAlarmSeverity(Link link, LinkType linkType) {
        log.debug("get link current alarm severity,link id is:{}", link.getLinkId());
        AlarmSeverity currenAlarmSeverity = AlarmSeverity.Unknown;
        switch (linkType) {
            case OCH_LINK:
                Och ochPhysical = link.getAugmentation(Link1.class).getOch();
                currenAlarmSeverity = ochPhysical.getAlarmState();
                break;
            case PHYSICAL_LINK:
                Physical phyLinkPhysical = link.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                        .getPhysical();
                currenAlarmSeverity = phyLinkPhysical.getAlarmState();
                break;
            case SITE_LINK:
                Site siteLinkPhysical = link.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                        .getSite();
                currenAlarmSeverity = siteLinkPhysical.getAlarmState();
                break;
        }
        return currenAlarmSeverity;
    }
}
