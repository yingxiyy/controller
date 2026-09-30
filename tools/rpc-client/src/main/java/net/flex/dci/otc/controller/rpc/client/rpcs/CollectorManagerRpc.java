/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/30 14:07
 */
public interface CollectorManagerRpc {

    void createTelemetryCollector(TelemetryServer telemetryServer);

    void deleteTelemetryCollector(TelemetryServer telemetryServer);
}
