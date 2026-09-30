package net.flex.dci.otn.controller.nms.nms.component.object;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.ObjectBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.ObjectKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetailBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetailKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 8/11/2023 4:00 PM
 */
@Slf4j
public abstract class AbstractNMSElementObjectDetail implements INMSElementObjectDetail {

    @Autowired
    protected DciTopologyCacheManager dciTopologyCacheManager;

    @Autowired
    protected SubNetTreeNodeDao subNetTreeNodeDao;

    protected Object updateFriendName(Object friendNameObject, String friendName) {
        log.debug("start to update the object friendName:{}", friendName);
        ObjectBuilder objectBuilder = new ObjectBuilder();
        objectBuilder.setFriendlyName(friendName);
        objectBuilder.setTopologyRef(friendNameObject.getTopologyRef());
        objectBuilder.setObjectType(friendNameObject.getObjectType());
        objectBuilder.setObjectId(friendNameObject.getObjectId());
        objectBuilder.setKey(
                new ObjectKey(objectBuilder.getObjectId(), objectBuilder.getTopologyRef()));
        return objectBuilder.build();
    }

    protected Object updateFriendName(Object friendNameObject, FriendInfoObjectDto friendInfoDto) {
        log.debug("start to update the object friendName:{}", friendInfoDto);
        ObjectBuilder objectBuilder = new ObjectBuilder();
        objectBuilder.setFriendlyName(friendInfoDto.getFriendName());
//        objectBuilder.setAlarmStatus(friendInfoDto.getAlarmState());
//        objectBuilder.setEventState(friendInfoDto.getEventState());
        objectBuilder.setTopologyRef(friendNameObject.getTopologyRef());
        objectBuilder.setObjectType(friendNameObject.getObjectType());
        objectBuilder.setObjectId(friendNameObject.getObjectId());
        objectBuilder.setKey(
                new ObjectKey(objectBuilder.getObjectId(), objectBuilder.getTopologyRef()));
        return objectBuilder.build();
    }


    protected ObjectDetail updateObjectDetail(ObjectDetail detail,
            ObjectDetailDto objectDetailDto) {
        log.debug("start to update the object detail:{}", detail);
        ObjectDetailBuilder objectBuilder = new ObjectDetailBuilder();
        objectBuilder.setFriendlyName(objectDetailDto.getFriendName());
        objectBuilder.setAlarmState(objectDetailDto.getAlarmState());
        objectBuilder.setAdminState(objectDetailDto.getAdminStatus());
        objectBuilder.setImplementState(objectDetailDto.getImplementState());
        objectBuilder.setAlignmentStatus(objectDetailDto.getAlignmentStatus());
        objectBuilder.setOperationalState(objectDetailDto.getOperStatus());
        objectBuilder.setTopologyRef(detail.getTopologyRef());
        objectBuilder.setObjectType(detail.getObjectType());
        objectBuilder.setObjectId(detail.getObjectId());
        objectBuilder.setSubnetId(objectDetailDto.getSubnetId());
        objectBuilder.setSubnetLevel(objectDetailDto.getSubnetLevel());
        objectBuilder.setSubnetName(objectDetailDto.getSubnetName());
        objectBuilder.setKey(
                new ObjectDetailKey(objectBuilder.getObjectId(), objectBuilder.getTopologyRef()));
        return objectBuilder.build();
    }

    protected SubNetTreeNode getSubnet(String subnetId) {
        log.debug("get subnet tree node by id:{}", subnetId);
        if (!StringUtils.hasText(subnetId)) {
            return null;
        }
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.NOT_FOUND_ERROR,
                        "subnet tree node " + subnetId + " is not found "));
        return subNetTreeNode;
    }
}
