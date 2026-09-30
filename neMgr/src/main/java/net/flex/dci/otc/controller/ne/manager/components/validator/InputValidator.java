package net.flex.dci.otc.controller.ne.manager.components.validator;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ExecuteNetconfCommandInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StartSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StopSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeInput;

/**
 * @version 1.0
 * @date 7/25/2023 1:19 PM
 */
public interface InputValidator {

    void validateCreateNeInput(CreateNeInput createNeInput);

    void validateStartSuperviseNeInput(StartSuperviseNeInput startSuperviseNeInput);

    void validateStopSuperviseNeInput(StopSuperviseNeInput stopSuperviseNeInput);

    void validateClearNeApsSwitchLogsInput(ClearNeApsSwitchLogsInput clearNeApsSwitchLogsInput);

    void validateManageApsSwitchInput(ManageApsSwitchInput manageApsSwitchInput);

    void validateOperationLink(NeOperationLinkInput neOperationLinkInput);

    void validateSwitchCuActiveStandby(SwitchCuActiveStandbyInput activeStandbyInput);

    void validateUploadHistoryPmInput(UploadHistoryPmInput uploadHistoryPmInput);

    void validateChannelAseRestoreInput(ChannelAseRestoreInput channelAseRestoreInput);

    void validateExecuteNetConfCmd(ExecuteNetconfCommandInput executeNetconfCommandInput);
}
