/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery1.impl;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.discovery.common.util.TaskInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.recover.output.Result;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.recover.output.ResultBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.recover.output.ResultKey;
import org.springframework.stereotype.Component;


//
// for each tunnel, send tti  on A side, and then Z side TTI recieved should be sent value.
//
@Component
@Slf4j
public class Discovery {


    private static Gson gson = new Gson();
    protected TaskInfo taskInfo;
//    private List<String> tunnelIdList;

//    public Discovery(List<String> resourceList) {
//        this.tunnelIdList = resourceList;
//    }

    public RecoverOutput recoverTunnel(RecoverInput input) {
        List<String> tunnelIdList = input.getResourceId();
        log.info("start to recover tunnel,the tunnel id is :{}", tunnelIdList);
        RecoverOutputBuilder output = new RecoverOutputBuilder().setResult(new ArrayList<>());

        for (String tunnelId : tunnelIdList) {
            DiscoveryImpl disc = DiscoveryImpl.getInstance();
            Result result;
            try {
                disc.doIt(tunnelId);
                result = new ResultBuilder()
                        .setResourceId(tunnelId)
                        .setKey(new ResultKey(tunnelId))
                        .setReturnCode(disc.getResult().getFinalState())
                        .build();
            } catch (CommonException ce) {
                log.error("failed to recover the tunnel:{} ,the reason is:{}", tunnelId,
                        ce.getMessage(), ce);
                result = new ResultBuilder()
                        .setResourceId(tunnelId)
                        .setKey(new ResultKey(tunnelId))
                        .setReturnCode(ImplementState.Allocate)
                        .setReturnMessage(ce.getMessage() + "...." + ce.getStackTrace().toString())
                        .build();
            }
            taskInfo.logMessage(disc.getResult());
            output.getResult().add(result);
        }
        return output.build();
    }

    public Discovery setTaskInfo(TaskInfoMessage taskInfoMessage) {
        taskInfo = new TaskInfo(taskInfoMessage);
        return this;
    }
}
