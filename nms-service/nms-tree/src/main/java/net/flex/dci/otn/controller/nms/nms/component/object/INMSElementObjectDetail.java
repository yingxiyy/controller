package net.flex.dci.otn.controller.nms.nms.component.object;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;

/**
 * @version 1.0
 * @date 8/11/2023 2:52 PM
 */
public interface INMSElementObjectDetail {

    Object getElementFriendNameObject(Object friendNameObj);

    ObjectType supportObjectType();

    ObjectDetail getElementObjectDetail(ObjectDetail detail);
}
