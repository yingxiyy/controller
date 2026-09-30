package net.flex.dci.otn.controller.allocate.designer.bom;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.BomMetaConfig;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTransceiverService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BomMetaService {

    @Autowired
    private OtTransceiverService otTransceiverService;

    @Autowired
    private BomMetaConfig bomMetaConfig;


    public String getBomMetaKey(Equipments equip, String vendorName, String vendorType) throws NeDesignerException {
        String equipTypeConfiged = getAdaptedEquipTypeConfiged(equip);

        String cardClass = null;
        if (equipTypeConfiged.equals("TRANSCEIVER-C")) {
            cardClass = otTransceiverService.getTransceiverClientMedium(equip);//e.g. ETH_100GBASE_LR4

        }
        return bomMetaConfig.getKey(vendorName, vendorType, equipTypeConfiged, cardClass);
    }


    private String getAdaptedEquipTypeConfiged(Equipments equip) {
        String equipTypeConfiged = equip.getEquipTypeConfiged();
        if (equipTypeConfiged != null && equipTypeConfiged.equals("TRANSCEIVER")) {
            if (equip.getFriendlyName().matches(".*-L\\d+")) {
                return new StringBuilder(equipTypeConfiged).append("-L").toString();
            } else if (equip.getFriendlyName().matches(".*-C\\d+")) {
                return new StringBuilder(equipTypeConfiged).append("-C").toString();
            }
        } else if (equip.getEquipType().equals(EquipType.EMPTY)){
            return "BLANK";
        }
        String equipVendor = equip.getEquipTypeVendorSpecific();
        if (equipVendor != null) {
            return equipVendor;
        }
        return equipTypeConfiged;
    }

}
