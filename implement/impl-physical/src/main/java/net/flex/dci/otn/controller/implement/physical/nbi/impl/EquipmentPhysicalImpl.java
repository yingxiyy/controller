package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutputBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/26 11:34
 */
@Component
@Slf4j
public class EquipmentPhysicalImpl extends BaseImpl {


    @Override
    public UpdateEquipOutput updateEquipPhysical(UpdateEquipInput input) {
        return new UpdateEquipOutputBuilder().build();
    }

}
