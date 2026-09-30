/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.Dijkstra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {

    private Set<DJNode> nodes = new HashSet<>();
    private Map<String, DJNode> nodeMap = new HashMap<>();

    public void addNode(String siteLinkId, DJNode node) {
        nodes.add(node);
        nodeMap.put(siteLinkId, node);
    }

    public Set<DJNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<DJNode> nodes) {
        this.nodes = nodes;
    }

    public DJNode getNodeByLinkId(String linkId) {
        return nodeMap.get(linkId);
    }
}