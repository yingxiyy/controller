package net.flex.dci.otn.controller.implement.physical.component;

import java.util.List;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;

/**
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
public interface ApsSwitchManager {

    void batchTunnelApsSwitch(List<String> tunnelIds, ApsSwitch apsSwitch, ApsPath targetApsPath,
            TaskInfoMessage taskInfoMessage);

    SwitchResult executeApsSwitch(String neId, String apsName, String apsCrossConnectionId,
            ApsPath targetPath, TaskInfoMessage taskInfoMessage);

    RestoreResult executeRestore(String neId, String apsName, String apsCrossConnectionId,
            TargetApsMember restoreMember, TaskInfoMessage restoreTaskInfo);
}
