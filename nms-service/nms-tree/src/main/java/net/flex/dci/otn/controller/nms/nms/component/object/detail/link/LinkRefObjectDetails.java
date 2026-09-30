package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import net.flex.dci.otn.controller.nms.nms.dto.FriendInfoObjectDto;
import net.flex.dci.otn.controller.nms.nms.dto.ObjectDetailDto;

/**
 * @version 1.0
 * @date 8/11/2023 3:35 PM
 */
public interface LinkRefObjectDetails {

    String getLinkName(String linkId);

    String supportTopologyId();

    FriendInfoObjectDto getLinkFriendInfo(String linkId);

    ObjectDetailDto getLinkObjectDetailInfo(String linkId);
}
