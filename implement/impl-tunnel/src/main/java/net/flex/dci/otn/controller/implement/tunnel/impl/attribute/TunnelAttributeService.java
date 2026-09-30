/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;


import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TunnelAttributeService extends BaseImpl {

    private final List<TunnelAttributeUpdateStrategy> updateStrategies;

    public TunnelAttributeService(List<TunnelAttributeUpdateStrategy> updateStrategies) {
        this.updateStrategies = updateStrategies;
    }


    @Override
    public RpcResultType updateTunnelAttribute(UpdateTunnelInput input,
            TaskInfoMessage taskInfoMessage) throws CommonException {
        if (input.getTunnelId() == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "please provide tunnel Id");
        }
        String tunnelId = input.getTunnelId();
        taskInfoMessage.setResourceId(input.getTunnelId());
        for (TunnelAttributeUpdateStrategy strategy : updateStrategies) {
            if (strategy.supports(input)) {
                try {
                    taskInfoMessage.setActionType(strategy.taskActionType());
                    strategy.execute(tunnelId, input, taskInfoMessage);
                } catch (CommonException e) {
                    log.error("error happen during tunnel attribute update", e);
                    throw e;
                }
            }
        }
        return RpcResultType.Success;
//        if (input.getFriendlyName() != null && !"".equals(input.getFriendlyName())) {
//            taskInfoMessage.setActionType(TaskInfoMessage.ActionType.updateFriendlyName);
//
//            try {
//                TunnelAttributeFriendlyNameUpdateStrategy.instance().setTaskInfo(taskInfoMessage);
//                TunnelAttributeFriendlyNameUpdateStrategy.instance()
//                        .updateFriendlyName(input.getTunnelId(), input.getFriendlyName());
//            } catch (CommonException e) {
//                log.error("error happen ", e);
//                throw e;
//            }
//        }
//
//        if (input.getFrequency() != null && !"".equals(input.getFrequency())) {
//            taskInfoMessage.setActionType(TaskInfoMessage.ActionType.changeFrequency);
//
//            try {
//                TunnelAttributeUpdateFrequencyStrategy.instance().setTaskInfo(taskInfoMessage);
//                TunnelAttributeUpdateFrequencyStrategy.instance()
//                        .updateFrequency(input.getTunnelId(), input.getFrequency());
//            } catch (CommonException e) {
//                log.error("error happen ", e);
//                throw e;
//            }
//        }
//
//        if (input.getModulationMode() != null && !"".equals(input.getModulationMode())) {
//            taskInfoMessage.setActionType(TaskInfoMessage.ActionType.changeOpMode);
//            try {
//                TunnelAttributeUpdateModulationModeStrategy.instance().setTaskInfo(taskInfoMessage);
//                TunnelAttributeUpdateModulationModeStrategy.instance().updateModulationMode(
//                        input.getTunnelId(),
//                        input.getModulationMode(),
//                        input.getLineSignalRate());
//            } catch (CommonException e) {
//                log.error("error happen ", e);
//                throw e;
//            }
//        }
//
//        if (input.getTTI() != null) {
//            taskInfoMessage.setActionType(TaskInfoMessage.ActionType.changeTTI);
//            try {
//                tunnelAttributeUpdateTTIStrategy.setTaskInfo(taskInfoMessage);
//                tunnelAttributeUpdateTTIStrategy.start(input.getTunnelId(), input.getTTI());
//            } catch (CommonException e) {
//                log.error("error happen ", e);
//                throw e;
//            }
//        }
//
//        if (input.getTestSignal() != null) {
//            taskInfoMessage.setActionType(TaskInfoMessage.ActionType.changeTestSignal);
//            try {
//                tunnelAttributeUpdateTestSignalStrategy.setTaskInfo(taskInfoMessage);
//                tunnelAttributeUpdateTestSignalStrategy.start(input.getTunnelId(),
//                        input.getTestSignal());
//            } catch (CommonException e) {
//                log.error("error happen ", e);
//                throw e;
//            }
//        }
//
//        return RpcResultType.Success;
    }
}
