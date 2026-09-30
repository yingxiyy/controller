/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.controller.rpc.client.dto.TelemetryServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerOutput;

/**
 * @version 1.0
 * @date 2021/12/20 11:06
 */
public interface TelemetryServerManagerRpc {

    /**
     * add a telemetry server
     *
     * @param telemetryServer
     * @return
     */
    CreateTelemetryServerOutput addTelemetryServer(TelemetryServer telemetryServer);


    /**
     * delete a telemetry server
     *
     * @param telemetryServer
     * @return
     */
    DeleteTelemetryServerOutput deleteTelemetryServer(TelemetryServer telemetryServer);
}
