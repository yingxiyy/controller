package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @version 1.0
 * @date 2022/12/27 10:38
 */
public interface WaveLengthSelectiveSwitch {


    List<Channel> getWssChannel(NeYangModel model, Equipments equipment,
            List<CrossConnections> refCrossConnections, List<String> phyLinkIds);
}
