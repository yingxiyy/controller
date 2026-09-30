/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Data
@Builder
public class OtNewOchNodeInfo {


    private Node lTpNode;

    private String lTp;
    private String lTp2;//for reg
    private Node lTp2Node;//for reg, may differ from lTpNode when user-defined cards are on different NEs

    private List<Node> snapshotNode;

    private String op6Tp;

    private Node op6Node;//通常情况下，op6Node和lTpNode应该是同一个，但是当有reusedIncludeNodes，可能会不同
}
