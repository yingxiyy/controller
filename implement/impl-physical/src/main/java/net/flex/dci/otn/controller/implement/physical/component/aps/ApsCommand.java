package net.flex.dci.otn.controller.implement.physical.component.aps;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsMember;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
public interface ApsCommand {

    SwitchResult executeCommand(String neId, CrossConnections apsCrossConnection, String apsName,
            CustomApsPath apsPath, TaskInfoMessage taskInfoMessage);

    default SwitchResult configAps(String refNeId, String apsCrossConnectionId,
            String apsName,
            ApsSwitch apsSwitch) {
        return null;
    }

    default RestoreResult executeRestoreCommand(String neId, CrossConnections apsCrossConnection,
            String apsName, ApsMember apsMember, TaskInfoMessage restoreTaskInfo) {
        return null;
    }
}
