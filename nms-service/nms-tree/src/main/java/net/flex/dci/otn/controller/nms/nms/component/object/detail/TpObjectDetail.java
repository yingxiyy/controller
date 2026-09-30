package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:08 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TpObjectDetail extends AbstractNMSElementObjectDetail {


    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        String tpId = friendNameObj.getObjectId();
        log.debug("start to get the tp friend name,the tp id is:{}", tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        TerminationPointCache terminationPointCache = dciTopologyCacheManager.getValue(tpId,
                TerminationPointCache.class);
//        Physical refTpPhysical = terminationPointDao.getTpPhysical(neId, tpId);
//        String friendlyName = null;
//        if (null == refTpPhysical) {
//            log.error("can not find the required tp ,the tp id is:{}", tpId);
//        }
        String friendlyName = terminationPointCache.getSimpleName();
        return updateFriendName(friendNameObj, friendlyName);
    }

    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Tp;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        return null;
    }
}
