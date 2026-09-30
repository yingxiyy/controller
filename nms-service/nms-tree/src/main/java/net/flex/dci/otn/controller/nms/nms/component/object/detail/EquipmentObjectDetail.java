package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
import net.flex.dci.otn.topology.cache.model.EquipmentCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:05 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentObjectDetail extends AbstractNMSElementObjectDetail {


    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        String eqId = friendNameObj.getObjectId();
        log.debug("start to get element friend name for equip,the equip id is:{}", eqId);
        EquipmentCache equipmentCache = dciTopologyCacheManager.getValue(eqId,
                EquipmentCache.class);
//        String refNeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
//        Equipments equipment = equipmentsDao.getEquipmentByNodeAndEqId(refNeId, eqId);
//        if (null == equipment) {
//            log.error("failed to get the equipment the equipment id is:{}", eqId);
//            return null;
//        }
        String equipmentFriendlyName = equipmentCache.getFriendlyName();
        Object equipmentFriendName = updateFriendName(friendNameObj, equipmentFriendlyName);
        return equipmentFriendName;
    }

    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Equip;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        return null;
    }
}
