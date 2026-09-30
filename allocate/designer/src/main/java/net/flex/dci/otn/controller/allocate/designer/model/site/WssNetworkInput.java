/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.site;

import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
public class WssNetworkInput {

    @NonNull
    private RouteInfo linkA;
    @NonNull
    private String linkAName;

    public String getLinkAPort() {
        if (linkAPort == null || linkAPort.isEmpty()) {
            return null;
        }
        return linkAPort.toUpperCase();
    }

    public String getLinkZPort() {
        if (linkZPort == null || linkZPort.isEmpty()) {
            return null;
        }
        return linkZPort.toUpperCase();
    }

    private String linkAPort;
    @NonNull
    private RouteInfo linkZ;
    @NonNull
    private String linkZName;
    private String linkZPort;
    @NonNull
    private String siteId;
    @NonNull
    Map<String, Node> totalNodesMap;
    @NonNull
    Set<Integer> usedDimensions;
}
