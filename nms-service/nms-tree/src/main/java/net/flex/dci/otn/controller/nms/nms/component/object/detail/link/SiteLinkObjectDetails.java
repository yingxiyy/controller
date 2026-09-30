package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:31 PM
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkObjectDetails extends AbstractLinkObjectDetails {


    private final SiteLinkDao siteLinkDao;

    @Override
    public String getLinkName(String linkId) {
        log.debug("start to get the site link name,the link id is:{}", linkId);
        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
        if (null == siteLink) {
            log.error("can not find the require site link,the site link id is:{}", linkId);
            return null;
        }
        Site sitePhysical = siteLink.getAugmentation(Link1.class).getSite();
        String friendlyName = sitePhysical.getFriendlyName();
        GridType grid = sitePhysical.getGrid();
        String bandwidth = sitePhysical.getBandwidth();
        String enrichedFriendName = NMSUtils.richSiteLinkName(friendlyName, grid, bandwidth);
        return enrichedFriendName;
    }

    @Override
    public String supportTopologyId() {
        return TopoNameConstants.Site_Topo_Key;
    }

    @Override
    public FriendInfoObjectDto getLinkFriendInfo(String linkId) {
        log.debug("start to get the site link friend info,the link id is:{}", linkId);
        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
        if (null == siteLink) {
            log.error("can not find the require site link,the site link id is:{}", linkId);
            return null;
        }
        Site sitePhysical = siteLink.getAugmentation(Link1.class).getSite();
        String friendlyName = sitePhysical.getFriendlyName();
        GridType gridType = sitePhysical.getGrid();
        String bandwidth = sitePhysical.getBandwidth();
        String enrichedFriendName = NMSUtils.richSiteLinkName(friendlyName, gridType, bandwidth);
        AlarmSeverity alarmSeverity = sitePhysical.getAlarmState();

        return FriendInfoObjectDto.builder().friendName(enrichedFriendName)
                .alarmState(alarmSeverity).build();
    }

    @Override
    public ObjectDetailDto getLinkObjectDetailInfo(String linkId) {
        log.debug("start to get the site link objectDetails info,the link id is:{}", linkId);
        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
        if (null == siteLink) {
            log.error("can not find the require site link,the site link id is:{}", linkId);
            return null;
        }
        Site sitePhysical = siteLink.getAugmentation(Link1.class).getSite();
        String friendlyName = sitePhysical.getFriendlyName();
        GridType gridType = sitePhysical.getGrid();
        String bandwidth = sitePhysical.getBandwidth();
        String subnetId = sitePhysical.getPlaneId();
        SubNetTreeNode subNetTreeNode = getSubnet(subnetId);
        String enrichedFriendName = NMSUtils.richSiteLinkName(friendlyName, gridType, bandwidth);
        AlarmSeverity alarmSeverity = sitePhysical.getAlarmState();
        ImplementState implementState = sitePhysical.getImplementState();
        AlignmentStatusType alignmentStatus = sitePhysical.getAlignmentStatus();
        OperStatus operationalStatus = sitePhysical.getOperationalState();
        AdminStatus adminStatus = sitePhysical.getAdminState();

        return ObjectDetailDto.builder().friendName(enrichedFriendName)
                .implementState(implementState)
                .alignmentStatus(alignmentStatus)
                .operStatus(operationalStatus)
                .adminStatus(adminStatus)
                .alarmState(alarmSeverity)
                .subnetId(subNetTreeNode == null ? null : subNetTreeNode.getSubNetId())
                .subnetName(subNetTreeNode == null ? null : subNetTreeNode.getName())
                .subnetLevel(subNetTreeNode == null ? null : subNetTreeNode.getLevel())
                .build();
    }
}
