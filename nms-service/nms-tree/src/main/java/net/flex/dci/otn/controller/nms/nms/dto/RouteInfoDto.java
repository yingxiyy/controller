/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * @version 1.0
 * @date 2021/12/23 16:49
 */
@Data
@Builder
public class RouteInfoDto {

    private String source;

    private String destination;

    private List<Route> routes;

    private String LinkType;

    private String connectionId;
}
