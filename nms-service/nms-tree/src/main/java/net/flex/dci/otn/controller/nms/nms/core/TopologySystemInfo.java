/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.SystemInfoHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateSystemInfoOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologySystemInfo extends BaseNms {

    public final String UPDATE_SYSTEM_INFO = "nms:update-system-info";

    @Autowired
    private SystemInfoHandler systemInfoHandler;

    public TopologySystemInfo(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(UPDATE_SYSTEM_INFO)) {
            returnValue = updateSystemInfo(cmd, requestBody);
        }
        return returnValue;
    }

    private String updateSystemInfo(String cmd, String requestBody) throws CommonException {
        try {
            UpdateSystemInfoOutput output = this.systemInfoHandler.updateSystemInfo();
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }

    }
}
