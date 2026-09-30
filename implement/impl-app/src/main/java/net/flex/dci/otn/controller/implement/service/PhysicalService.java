package net.flex.dci.otn.controller.implement.service;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Export1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutput;

/**
 * @version 1.0
 * @date 2022/8/21 13:48
 */

public interface PhysicalService {

    UpdateTerminationPointOutput updateTerminationPoint(
            UpdateTerminationPointInput updateTerminationPointInput,
            TaskInfoMessage taskInfoMessage);

    void export1524TelemetryData(Export1524TelemetryDataInput input) throws CommonException;

    TestConnectionStatusOutput testNeConnectionStatus(TestConnectionStatusInput input)
            throws CommonException;

    UpdateEquipOutput updateEquipment(UpdateEquipInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException;


    GetNeDataOutput getNeData(GetNeDataInput input);

    RemoveNeOutput removeNe(RemoveNeInput input, TaskInfoMessage taskInfoMessage);

    CreateScanLinkOutput createScanLink(CreateScanLinkInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException;

    DeleteScanLinkOutput deleteScanLink(DeleteScanLinkInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException;

    UpdateLinkOutput updatePhyLink(UpdateLinkInput input, TaskInfoMessage taskInfoMessage);

    UpdateNodeOutput updateNe(UpdateNodeInput input, TaskInfoMessage taskInfoMessage);

    UpdateCrossConnectionOutput updateCrossConnections(UpdateCrossConnectionInput input,
            TaskInfoMessage taskInfoMessage);

    RemoveIpOutput removeNeIp(RemoveIpInput input, TaskInfoMessage taskInfoMessage);

    SwitchNeCuActiveStandbyOutput switchNeCuActiveStandby(SwitchNeCuActiveStandbyInput input,
            TaskInfoMessage taskInfoMessage);

    UploadNeHistoryPmOutput uploadNeHistoryPm(UploadNeHistoryPmInput input,
            TaskInfoMessage taskInfoMessage);
}
