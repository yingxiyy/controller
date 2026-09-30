package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum;

import static net.flex.dci.otn.controller.nms.utils.Constants.ASE_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otn.controller.nms.nms.comparator.OchLinkSortComparator;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux48;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux64;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux96;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MuxFlex;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MuxFlex32CL;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MuxFlexPB64;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.PMUX64;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 7/9/2025 3:38 PM
 */
@Component
@Slf4j
public class FixedMuxConstructor implements MuxConstructor {

    @Override
    public MUX constructMux(NeYangModel model, Equipments muxEquipments, List<Link> ochLinks) {
        log.debug("construct fixed mux muxEquipments :{}", muxEquipments);
        String equipTypeConfig = muxEquipments.getEquipClass();
        MUX mux = constructFixedMuxByEquipTypeConfig(equipTypeConfig,
                muxEquipments.getEquipmentId(), model, ochLinks);
        return mux;
    }

    private MUX constructFixedMuxByEquipTypeConfig(String equipTypeConfig, String equipmentId,
            NeYangModel model,
            List<Link> ochLinks) {
        MUX mux = null;
        if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_FLEX_32CL) || equipTypeConfig.contains(
                Constants.MUX_SPECTRUM_FLEX_32C32L)) {
            List<Link> realOchList = ochLinks.stream()
                    .filter(link -> !link.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                            .getOch().getFriendlyName()
                            .startsWith(ASE_PREFIX)).sorted(new OchLinkSortComparator(model))
                    .collect(Collectors.toList());
            mux = new MuxFlex32CL(model, equipmentId, realOchList);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_FLEX_PB64)) {
            mux = new MuxFlexPB64(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_PMUX64)
                || equipTypeConfig.contains(Constants.MUX_SPECTRUM_PMUXC64)) {
            mux = new PMUX64(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_FLEX)) {
            ochLinks.sort(new OchLinkSortComparator(model));
            mux = new MuxFlex(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_48)) {
            mux = new Mux48(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_64)) {
            mux = new Mux64(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_96)) {
            mux = new Mux96(model, equipmentId, ochLinks);
        } else {
            String msg = String.format("unknown equip class %s", equipTypeConfig);
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR, msg);
        }
        return mux;
    }

    @Override
    public List<EquipType> supportEquipType() {
        return Arrays.asList(EquipType.MUX32CL, EquipType.MUX, EquipType.MUXPANEL,
                EquipType.CMUX64);
    }
}
