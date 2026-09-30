package net.flex.dci.otn.controller.implement.physical.validator;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsSwitchControlInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.BatchApsSwitchInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;

/**
 *
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
public interface InputValidator {

    void validateBatchApsSwitchInput(BatchApsSwitchInput batchApsSwitchInput);

    void validateApsSwitchControlInput(ApsSwitchControlInput apsSwitchControlInput);

    void validateUploadNeHistoryPmInput(UploadNeHistoryPmInput input);

    void validateRestoreApsPathInput(RestoreApsPathInput restoreApsPathInput);
}
