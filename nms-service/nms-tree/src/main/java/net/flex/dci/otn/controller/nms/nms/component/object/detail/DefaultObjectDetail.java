package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.INMSElementObjectDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.ObjectBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:22 PM
 */
@Component
@Slf4j
public class DefaultObjectDetail implements INMSElementObjectDetail {

    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        log.debug("no handler to handle get friend name object,do default");
        ObjectBuilder objectBuilder = new ObjectBuilder(friendNameObj);
        objectBuilder.setFriendlyName(null);
        return objectBuilder.build();
    }

    @Override
    public ObjectType supportObjectType() {
        return null;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        return null;
    }
}
