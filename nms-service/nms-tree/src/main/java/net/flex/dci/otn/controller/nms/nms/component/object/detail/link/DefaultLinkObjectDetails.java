package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;

/**
 * @version 1.0
 * @date 8/11/2023 3:53 PM
 */
@Slf4j
public class DefaultLinkObjectDetails extends AbstractLinkObjectDetails {

    @Override
    public String getLinkName(String linkId) {
        log.debug("get default link name,the default name is null");
        return null;
    }

    @Override
    public String supportTopologyId() {
        return null;
    }

    @Override
    public FriendInfoObjectDto getLinkFriendInfo(String linkId) {
        return null;
    }

    @Override
    public ObjectDetailDto getLinkObjectDetailInfo(String linkId) {
        return null;
    }
}
