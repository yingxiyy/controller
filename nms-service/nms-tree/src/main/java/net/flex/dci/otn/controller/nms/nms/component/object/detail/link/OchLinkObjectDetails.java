package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import net.flex.dci.otn.topology.cache.model.OchLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:32 PM
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OchLinkObjectDetails extends AbstractLinkObjectDetails {

    private final OchLinkDao ochLinkDao;

    @Override
    public String getLinkName(String linkId) {
        log.debug("start to get the och link,link id:{} name", linkId);
        OchLinkCache ochLinkCache = dciTopologyCacheManager.getValue(linkId, OchLinkCache.class);
//        Link ochLink = ochLinkDao.getOchLinkByLinkId(linkId);
//        if (null == ochLink) {
//            log.error("can not find the require och link,the och link id is:{}", linkId);
//            return null;
//        }
//        Och ochLinkPhysical = ochLink.getAugmentation(
//                Link1.class).getOch();

        return ochLinkCache.getFriendlyName();
    }

    @Override
    public String supportTopologyId() {
        return TopoNameConstants.Och_Topo_Key;
    }

    @Override
    public FriendInfoObjectDto getLinkFriendInfo(String linkId) {
        log.debug("start to get the och link,link id:{} name", linkId);
        Link ochLink = ochLinkDao.getOchLinkByLinkId(linkId);
        if (null == ochLink) {
            log.error("can not find the require och link,the och link id is:{}", linkId);
            return null;
        }
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        String friendlyName = och.getFriendlyName();
        AlarmSeverity alarmState = och.getAlarmState();

        return FriendInfoObjectDto.builder().friendName(friendlyName).alarmState(alarmState)
                .build();
    }

    @Override
    public ObjectDetailDto getLinkObjectDetailInfo(String linkId) {
        log.debug("start to get the och link,link id:{} name", linkId);
        Link ochLink = ochLinkDao.getOchLinkByLinkId(linkId);
        if (null == ochLink) {
            log.error("can not find the require och link,the och link id is:{}", linkId);
            return null;
        }
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        String subnetId = och.getPlaneId();
        SubNetTreeNode subNetTreeNode = getSubnet(subnetId);
        String friendlyName = och.getFriendlyName();
        AlarmSeverity alarmState = och.getAlarmState();
        AlignmentStatusType alignmentStatus = och.getAlignmentStatus();
        AdminStatus adminStatus = och.getAdminState();
        ImplementState implementState = och.getImplementState();
        OperStatus operStatus = och.getOperationalState();

        return ObjectDetailDto.builder().friendName(friendlyName).alarmState(alarmState)
                .implementState(implementState)
                .alignmentStatus(alignmentStatus)
                .adminStatus(adminStatus)
                .operStatus(operStatus)
                .subnetId(subNetTreeNode == null ? null : subNetTreeNode.getSubNetId())
                .subnetName(subNetTreeNode == null ? null : subNetTreeNode.getName())
                .subnetLevel(subNetTreeNode == null ? null : subNetTreeNode.getLevel())
                .build();
    }
}
