/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/12/20 11:10
 */
@Data
@Builder
public class TelemetryServer {

    private String ipv4Address;

    private Integer port;

    private String apiVersion;

    private String telemetryVersion;

    private List<String> supportedNeVersion;

    private String name;


}
