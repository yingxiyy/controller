package net.flex.dci.otn.controller.implement.physical.component.aps;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class ManualApsCommand extends AbstractApsCommand {

    public ManualApsCommand(NeManagerRpc neManagerRpc, CrossConnectionsDao crossConnectionsDao,
            ApsTaskMessageHandler apsTaskMessageHandler) {
        super(neManagerRpc, crossConnectionsDao, apsTaskMessageHandler);
    }

    @Override
    public ApsSwitchMode switchMode() {
        return ApsSwitchMode.MANUEL;
    }

    @Override
    public SwitchResult executeCommand(String neId, CrossConnections apsCrossConnection,
            String apsName,
            CustomApsPath apsPath, TaskInfoMessage taskInfo) {
        log.debug("execute manual aps command neId:{} apsName:{} apsPath:{}", neId, apsName,
                apsPath);

//        Short index = apsPath.getIndex();
        ApsPathType path = apsPath.getApsRealPath();
        Action action = apsPath.getAction();
        Short index = getApsCommandSwitchRealIndex(apsCrossConnection, apsPath);
        ManageApsSwitchOutput result = neManagerRpc.manageApsSwitch(neId, apsName, path, index,
                action);
        SwitchResult switchResult = SwitchResult.builder().build();
        if (result.getReturnCode().equals(RpcResultType.Success)) {
            switchResult.setCode(SetResultCode.SUCCESS);
            switchResult.setMessage(result.getReturnMessage());
        } else {
            switchResult.setCode(SetResultCode.FAILED);
            switchResult.setMessage(result.getReturnMessage());
        }
        if (taskInfo != null) {
            TaskInfoMessage switchTaskInfo = apsTaskMessageHandler.enrichApsCommandInfo(
                    taskInfo, neId, apsName, apsPath, ApsSwitchMode.MANUEL);
            apsTaskMessageHandler.logApsSwitchResult(switchTaskInfo, switchResult);
        }
        return switchResult;
    }
}
