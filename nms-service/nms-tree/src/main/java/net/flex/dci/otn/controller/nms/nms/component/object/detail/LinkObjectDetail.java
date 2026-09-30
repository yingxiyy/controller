package net.flex.dci.otn.controller.nms.nms.component.object.detail;

import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.object.AbstractNMSElementObjectDetail;
import net.flex.dci.otn.controller.nms.nms.component.object.detail.link.DefaultLinkObjectDetails;
import net.flex.dci.otn.controller.nms.nms.component.object.detail.link.LinkRefObjectDetails;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 3:04 PM
 */
@Component
@Slf4j
public class LinkObjectDetail extends AbstractNMSElementObjectDetail {

    private HashMap<String, LinkRefObjectDetails> linkRefFriendNameHandleMap = new HashMap<>();

    public LinkObjectDetail(List<LinkRefObjectDetails> linkRefObjectDetails) {
        linkRefFriendNameHandleMap = linkRefObjectDetails.stream().collect(HashMap::new,
                (map, linkFriendName) -> map.put(linkFriendName.supportTopologyId(),
                        linkFriendName), HashMap::putAll);
    }

    @Override
    public Object getElementFriendNameObject(Object friendNameObj) {
        log.debug("start to get the link friend name ,the request object is :{}", friendNameObj);
        String linkId = friendNameObj.getObjectId();
        String topologyId = friendNameObj.getTopologyRef().getValue();
//        String friendName = linkRefFriendNameHandleMap.getOrDefault(topologyId,
//                new DefaultLinkFriendName()).getLinkName(linkId);
        FriendInfoObjectDto friendInfoDto = linkRefFriendNameHandleMap.getOrDefault(
                topologyId,
                new DefaultLinkObjectDetails()).getLinkFriendInfo(linkId);
        Object linkFriendNameObject = updateFriendName(friendNameObj, friendInfoDto);
        return linkFriendNameObject;
    }


    @Override
    public ObjectType supportObjectType() {
        return ObjectType.Link;
    }

    @Override
    public ObjectDetail getElementObjectDetail(ObjectDetail detail) {
        log.debug("start to get the element for link,the request object is:{}", detail);
        String linkId = detail.getObjectId();
        String topologyId = detail.getTopologyRef().getValue();
        ObjectDetailDto objectDetailDto = linkRefFriendNameHandleMap.getOrDefault(topologyId,
                new DefaultLinkObjectDetails()).getLinkObjectDetailInfo(linkId);
        ObjectDetail objectDetail = updateObjectDetail(detail, objectDetailDto);
        return objectDetail;
    }


}
