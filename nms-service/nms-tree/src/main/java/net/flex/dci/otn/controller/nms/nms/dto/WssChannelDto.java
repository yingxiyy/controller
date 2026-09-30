package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;

/**
 * @version 1.0
 * @date 2022/7/8 13:10
 */
@Data
@Builder
public class WssChannelDto implements Serializable {

    private VoaThresholdDto voaThresholdDto;

    private List<Channel> wssChannels;
}
