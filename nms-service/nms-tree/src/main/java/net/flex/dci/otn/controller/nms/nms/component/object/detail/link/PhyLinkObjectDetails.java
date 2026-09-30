package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import net.flex.dci.otn.topology.cache.model.PhyLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:32 PM
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkObjectDetails extends AbstractLinkObjectDetails {

    private final PhyLinkDao phyLinkDao;

    @Override
    public String getLinkName(String linkId) {
        log.debug("start to get the phy link friend name,the id is:{}", linkId);
        PhyLinkCache phyLinkCache = dciTopologyCacheManager.getValue(linkId, PhyLinkCache.class);
        return phyLinkCache.getFriendlyName();
    }

    @Override
    public String supportTopologyId() {
        return TopoNameConstants.Phy_Topo_Key;
    }

    @Override
    public FriendInfoObjectDto getLinkFriendInfo(String linkId) {
        log.debug("start to get the phy link friend name,the id is:{}", linkId);
        Link phyLink = phyLinkDao.getPhyLinkByLinkId(
                linkId);
        if (null == phyLink) {
            log.error("can not find the require phy link,the phy link id is:{}", linkId);
            return null;
        }
        Physical phyLinkPhysical = phyLink.getAugmentation(
                Link1.class).getPhysical();
        String friendName = phyLinkPhysical.getFriendlyName();
        AlarmSeverity alarmSeverity = phyLinkPhysical.getAlarmState();

        return FriendInfoObjectDto.builder().alarmState(alarmSeverity).friendName(friendName)
                .build();
    }

    @Override
    public ObjectDetailDto getLinkObjectDetailInfo(String linkId) {
        log.debug("get the phy link object details info the link id:{}", linkId);
        Link phyLink = phyLinkDao.getPhyLinkById(linkId);
        if (null == phyLink) {
            log.error("can not find the required phy link,the phy link is id :{}", linkId);
            return null;
        }
        Physical phyLinkPhysical = phyLink.getAugmentation(
                Link1.class).getPhysical();
        String friendName = phyLinkPhysical.getFriendlyName();
        AlarmSeverity alarmSeverity = phyLinkPhysical.getAlarmState();
        AdminStatus adminStatus = phyLinkPhysical.getAdminState();
        ImplementState implementState = phyLinkPhysical.getImplementState();
        OperStatus operationalStatus = phyLinkPhysical.getOperationalState();
        AlignmentStatusType alignmentStatusType = phyLinkPhysical.getAlignmentStatus();
        String subnetId = phyLinkPhysical.getPlaneId();
        SubNetTreeNode subNetTreeNode = getSubnet(subnetId);
        return ObjectDetailDto.builder().alarmState(alarmSeverity).friendName(friendName)
                .operStatus(operationalStatus)
                .alignmentStatus(alignmentStatusType)
                .adminStatus(adminStatus)
                .implementState(implementState)
                .subnetId(subNetTreeNode == null ? null : subNetTreeNode.getSubNetId())
                .subnetName(subNetTreeNode == null ? null : subNetTreeNode.getName())
                .subnetLevel(subNetTreeNode == null ? null : subNetTreeNode.getLevel())
                .build();
    }
}
