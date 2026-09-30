package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:05 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelObjectDetail extends AbstractNMSElementObjectDetail {

    private final TunnelDao tunnelDao;

    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        String tunnelId = friendNameObj.getObjectId();
        log.debug("start to get the tunnel friend name,the tunnel id is:{}", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            log.error("can not find the require tunnel ,the tunnel id is :{}", tunnelId);
            return friendNameObj;
        }
        String friendName = tunnel.getFriendlyName();
        return updateFriendName(friendNameObj, friendName);
    }

    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Tunnel;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        String tunnelId = detail.getObjectId();
        log.debug("start to get the tunnel friend name,the tunnel id is:{}", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            log.error("can not find the require tunnel ,the tunnel id is :{}", tunnelId);
            return detail;
        }
        String subnetId = tunnel.getPlaneId();
        SubNetTreeNode subNetTreeNode = getSubnet(subnetId);
        String friendName = tunnel.getFriendlyName();
        ImplementState implementState = tunnel.getImplementState();
        OperStatus operStatus = tunnel.getOperationalState();
        AdminStatus adminStatus = tunnel.getAdminState();
        AlignmentStatusType alignmentStatus = tunnel.getAlignmentStatus();
        AlarmSeverity alarmSeverity = tunnel.getAlarmState();
        ObjectDetailDto.ObjectDetailDtoBuilder objectDetailDtoBuilder = ObjectDetailDto.builder();

        objectDetailDtoBuilder.friendName(friendName)
                .implementState(implementState).operStatus(operStatus).adminStatus(adminStatus)
                .alignmentStatus(alignmentStatus).alarmState(alarmSeverity);
        if (subNetTreeNode != null) {
            objectDetailDtoBuilder.subnetName(subNetTreeNode.getName())
                    .subnetId(subNetTreeNode.getSubNetId())
                    .subnetLevel(subNetTreeNode.getLevel());

        }
        return updateObjectDetail(detail, objectDetailDtoBuilder.build());
    }
}
