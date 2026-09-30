package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @version 1.0
 * @date 7/9/2025 3:37 PM
 */
public interface MuxConstructor {

    MUX constructMux(NeYangModel model, Equipments muxEquipments, List<Link> ochLinks);

    List<EquipType> supportEquipType();
}
