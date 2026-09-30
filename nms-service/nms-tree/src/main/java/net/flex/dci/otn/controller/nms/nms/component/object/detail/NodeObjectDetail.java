package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
import net.flex.dci.otn.controller.nms.nms.component.object.detail.node.DefaultNodeFriendName;
import net.flex.dci.otn.controller.nms.nms.component.object.detail.node.NeFriendName;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:10 PM
 */
@Slf4j
@Component
public class NodeObjectDetail extends AbstractNMSElementObjectDetail {

    private Map<String, NeFriendName> neFriendNameMap = new HashMap<>();

    public NodeObjectDetail(List<NeFriendName> neFriendNames) {
        neFriendNameMap = neFriendNames.stream().collect(HashMap::new,
                (map, neFriendName) -> map.put(neFriendName.supportTopologyId(), neFriendName),
                HashMap::putAll);
    }

    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        log.debug("start to get the link friend name ,the request object is :{}", friendNameObj);
        String neId = friendNameObj.getObjectId();
        String topologyId = friendNameObj.getTopologyRef().getValue();
        String friendName = neFriendNameMap.getOrDefault(topologyId,
                new DefaultNodeFriendName()).getNodeName(neId);
        Object nodeFriendName = updateFriendName(friendNameObj, friendName);
        return nodeFriendName;
    }

    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Node;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        return null;
    }
}
