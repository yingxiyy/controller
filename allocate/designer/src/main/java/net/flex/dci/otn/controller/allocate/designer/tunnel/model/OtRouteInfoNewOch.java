/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

@Builder
@Getter
@ToString
public class OtRouteInfoNewOch {

    /**
     * 这里的snapshot主要是针对TPC重用的node，所以一条OCH最多只会有一个TPC snapshotNode。 如果没重用，就没有
     *
     * 但是如果有OP6卡的话，可能会重用2个node，所以这里定义成list
     */
    @NonNull
    private List<Node> snapshotNodes;
    @NonNull
    private Map<String, Node> inMemoryNodes;
    @NonNull
    private List<Link> osLinks;//exist between TPC node and siteLink

    @NonNull
    private List<CrossConnections> tpcXcs;
    @NonNull
    private List<CrossConnections> ochXcs;

    private String secondaryTp;//当allocate primary时，同时也可以选定secondary的TP
    private String thirdTp;//当allocate primary时，同时也可以选定third的TP

    Pair<String, String> muxMdPortTpSrcPair;
    @Builder.Default
    private List<String> removedResourceIds = Collections.emptyList();

}
