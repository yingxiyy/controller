package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.mongo.dto.OchLinkBriefInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

/**
 * @version 1.0
 * @date 2022/12/27 11:06
 */
public interface WaveLengthSelectiveSwitchCard {

    List<Channel> getCardWssChannel(NeYangModel model, List<CrossConnections> crossConnections,
            List<OchLinkBriefInfo> ochLinks);


    EquipType[] getCardTypes();
}
