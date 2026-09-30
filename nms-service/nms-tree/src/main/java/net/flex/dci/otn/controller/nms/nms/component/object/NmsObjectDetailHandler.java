package net.flex.dci.otn.controller.nms.nms.component.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.detail.DefaultObjectDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:09 PM
 */
@Slf4j
@Component
public class NmsObjectDetailHandler {

    private Map<ObjectType, INMSElementObjectDetail> operationsMap = new HashMap<>();

    public NmsObjectDetailHandler(List<INMSElementObjectDetail> operations) {
        operationsMap = operations.stream().collect(HashMap::new,
                (map, operation) -> map.put(operation.supportObjectType(), operation),
                HashMap::putAll);
    }

    public Object getObjectFriendName(Object friendlyObject) {
        log.debug("start to get the object friend name,the friendly object is :{}", friendlyObject);
        Object detailFriendObject = operationsMap.getOrDefault(friendlyObject.getObjectType(),
                new DefaultObjectDetail()).getElementFriendNameObject(friendlyObject);
        return detailFriendObject;
    }

    public ObjectDetail getObjectDetail(ObjectDetail detail) {
        log.debug("start to get the object detail,the object is :{}", detail);
        ObjectDetail detailInfo = operationsMap.getOrDefault(detail.getObjectType(),
                new DefaultObjectDetail()).getElementObjectDetail(detail);
        return detailInfo;
    }
}
