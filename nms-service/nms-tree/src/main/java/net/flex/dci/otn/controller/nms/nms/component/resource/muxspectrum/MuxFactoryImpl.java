package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 7/9/2025 3:27 PM
 */
@Component
@Slf4j
public class MuxFactoryImpl implements MuxFactory {

    private final Map<EquipType, MuxConstructor> equipTypeMuxConstructorMap;

    public MuxFactoryImpl(List<MuxConstructor> muxConstructors) {
        this.equipTypeMuxConstructorMap = muxConstructors.stream()
                .flatMap(ctor -> ctor.supportEquipType().stream()
                        .map(type -> new SimpleEntry<>(type, ctor)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new IllegalStateException("duplicated register " + a);
                        }
                ));
    }

    @Override
    public MUX constructMux(NeYangModel model, Equipments equipment, List<Link> ochLinks) {
        log.debug("construct Mux by yang mode:{} equipment id:{}", model,
                equipment.getEquipmentId());
        EquipType equipType = equipment.getEquipType();
        return this.equipTypeMuxConstructorMap.getOrDefault(equipType, null)
                .constructMux(model, equipment, ochLinks);
    }
}
