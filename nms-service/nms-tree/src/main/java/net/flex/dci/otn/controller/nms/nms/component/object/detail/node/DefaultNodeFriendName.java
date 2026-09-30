package net.flex.dci.otn.controller.nms.nms.component.object.detail.node;

import lombok.extern.slf4j.Slf4j;

/**
 * @version 1.0
 * @date 8/11/2023 5:11 PM
 */
@Slf4j
public class DefaultNodeFriendName implements NeFriendName {

    @Override
    public String getNodeName(String neId) {
        log.debug("get default ne name,the default name is null");
        return null;
    }

    @Override
    public String supportTopologyId() {
        return null;
    }
}
