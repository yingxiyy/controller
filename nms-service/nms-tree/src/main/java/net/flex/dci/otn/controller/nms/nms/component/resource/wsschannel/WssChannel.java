package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel;

import net.flex.dci.otn.controller.nms.nms.dto.WssChannelDto;

/**
 * @version 1.0
 * @date 2022/6/27 16:24
 */
public interface WssChannel {

    WssChannelDto retrieveRefEquipAllChannels(String nodeId);

    WssChannelDto retrieveRefEquipAllChannels(String nodeId, String equipmentId);
}
