/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.dfs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DFSNode {

    String siteLinkId;
    Map<String, String> neighborEdges; // 连接相邻节点的wssLink
    List<Long> frequencies;

    public DFSNode(String siteLinkId, List<Long> frequencies) {
        this.siteLinkId = siteLinkId;
        this.frequencies = frequencies;
        neighborEdges = new HashMap<>();
    }
    void addNeighbor(String neighbor, String wssLinkId) {
        this.neighborEdges.put(neighbor, wssLinkId);
    }

}
