package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
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
public class RackObjectDetail extends AbstractNMSElementObjectDetail {

    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        return null;
    }

    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Rack;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        return null;
    }
}
