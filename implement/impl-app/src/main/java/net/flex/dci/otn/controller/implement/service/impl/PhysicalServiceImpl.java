package net.flex.dci.otn.controller.implement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.CrossConnectionManager;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.Equipment;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.GetNeData;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.NeImpl;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.NeIpManager;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.NePhysical;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.PhysicalLink;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.RemoveNe;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.ScanLink;
import net.flex.dci.otn.controller.implement.physical.nbi.impl.TerminationPointNeImpl;
import net.flex.dci.otn.controller.implement.service.PhysicalService;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/8/21 14:11
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PhysicalServiceImpl implements PhysicalService {

    private final TerminationPointNeImpl terminationPoint;

    private final NeImpl ne;

    private final Equipment equipmentPhysical;

    private final RemoveNe removeNe;

    private final GetNeData getNeData;

    private final ScanLink scanLink;

    private final PhysicalLink physicalLink;

    private final NePhysical nePhysical;

    private final CrossConnectionManager crossConnectionManager;

    private final NeIpManager neIpManager;

    @Override
    public UpdateTerminationPointOutput updateTerminationPoint(
            UpdateTerminationPointInput updateTerminationPointInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("start to update termination point,termination point is :{}",
                updateTerminationPointInput);
        terminationPoint.setTaskInfo(taskInfoMessage);
        UpdateTerminationPointOutput output = terminationPoint.updateTerminationPoint(
                updateTerminationPointInput);
        return output;
    }

    @Override
    public void export1524TelemetryData(Export1524TelemetryDataInput input) throws CommonException {
        log.info("start to export the 15min 24h telemetry data");
        ne.export1524TelemetryData(input);
    }

    @Override
    public TestConnectionStatusOutput testNeConnectionStatus(TestConnectionStatusInput input)
            throws CommonException {
        log.info("start to test ne connection status,the input is:{}", input);
        TestConnectionStatusOutput output = ne.testNeConnectionStatus(input);
        return output;
    }

    @Override
    public UpdateEquipOutput updateEquipment(UpdateEquipInput input,
            TaskInfoMessage taskInfoMessage)
            throws CommonException {
        log.info("start to update equipment config ,the input is:{}", input);
        equipmentPhysical.setTaskInfo(taskInfoMessage);
        UpdateEquipOutput updateEquipOutput = equipmentPhysical.updatePhyEquipment(input);
        return updateEquipOutput;
    }

    @Override
    public GetNeDataOutput getNeData(GetNeDataInput input) {
        log.debug("start to get the ne data,the input is:{}", input);
        GetNeDataOutput neData = ne.getNeData(input);
        return neData;
    }

    @Override
    public RemoveNeOutput removeNe(RemoveNeInput input, TaskInfoMessage taskInfoMessage) {
        log.info("start to remove the ne,the input is:{}", input);
        removeNe.setTaskInfo(taskInfoMessage);
        RemoveNeOutput removeNeOutput = removeNe.removeNe(input);
        return removeNeOutput;
    }

    @Override
    public CreateScanLinkOutput createScanLink(CreateScanLinkInput input,
            TaskInfoMessage taskInfoMessage) throws CommonException {
        log.info("start to create the scan link,the input is:{}", input);
        scanLink.setTaskInfo(taskInfoMessage);
        CreateScanLinkOutput createScanLinkOutput = scanLink.createLink(input);
        return createScanLinkOutput;
    }

    @Override
    public DeleteScanLinkOutput deleteScanLink(DeleteScanLinkInput input,
            TaskInfoMessage taskInfoMessage) throws CommonException {
        log.info("start to delete the scan link,the input is:{}", input);
        scanLink.setTaskInfo(taskInfoMessage);
        DeleteScanLinkOutput deleteScanLinkOutput = scanLink.deleteLink(input);
        return deleteScanLinkOutput;
    }

    @Override
    public org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkOutput updatePhyLink(
            UpdateLinkInput input, TaskInfoMessage taskInfoMessage) {
        log.info("update physical link state ,the input is:{}", input);
        physicalLink.setTaskInfo(taskInfoMessage);
        UpdateLinkOutput updateLinkOutput = physicalLink.updatePhyLink(input);
        UpdateLinkOutputBuilder updateLinkOutputBuilder = new UpdateLinkOutputBuilder().setReturnCode(
                        updateLinkOutput.getReturnCode())
                .setReturnMessage(updateLinkOutput.getReturnMessage());
        return updateLinkOutputBuilder.build();
    }

    @Override
    public UpdateNodeOutput updateNe(UpdateNodeInput input, TaskInfoMessage taskInfoMessage) {
        log.info("update ne physical,the input is:{}", input);
//        nePhysical.setTaskInfo(taskInfoMessage);
        UpdateNodeOutput updateNodeOutput = nePhysical.updateNe(input, taskInfoMessage);
        return updateNodeOutput;
    }

    @Override
    public UpdateCrossConnectionOutput updateCrossConnections(UpdateCrossConnectionInput input,
            TaskInfoMessage taskInfoMessage) {
        log.info("update cross connection,the input is:{}", input);
        UpdateCrossConnectionOutput updateCrossConnectionOutput = crossConnectionManager.updateCrossConnections(
                input, taskInfoMessage);
        return updateCrossConnectionOutput;
    }

    @Override
    public RemoveIpOutput removeNeIp(RemoveIpInput input, TaskInfoMessage taskInfoMessage) {
        log.info("remove ne ip,the input is:{}", input);
        neIpManager.setTaskInfo(taskInfoMessage);
        RemoveIpOutput removeIpOutput = neIpManager.removeNeIp(input);
        return removeIpOutput;
    }

    @Override
    public SwitchNeCuActiveStandbyOutput switchNeCuActiveStandby(SwitchNeCuActiveStandbyInput input,
            TaskInfoMessage taskInfoMessage) {
        log.info("switch ne cu active standby,the input is:{}", input);
        ne.setTaskInfo(taskInfoMessage);
        SwitchNeCuActiveStandbyOutput output = ne.switchNeCu(input);
        return output;
    }

    @Override
    public UploadNeHistoryPmOutput uploadNeHistoryPm(UploadNeHistoryPmInput input,
            TaskInfoMessage taskInfoMessage) {
        log.info("upload ne history pm,the input is :{}", input);
        UploadNeHistoryPmOutput uploadNeHistoryPmOutput = ne.uploadHistoryPm(input,
                taskInfoMessage);
        return uploadNeHistoryPmOutput;
    }
}
