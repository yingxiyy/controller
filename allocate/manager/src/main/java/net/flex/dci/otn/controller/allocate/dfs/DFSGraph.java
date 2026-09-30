/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.dfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class DFSGraph {


    private Map<String, DFSNode> nodes;

    public DFSGraph() {
        this.nodes = new HashMap<>();
    }

    public void addNode(DFSNode node) {
        nodes.put(node.siteLinkId, node);
    }

    public void addEdge(String siteLink1, String siteLink2, String wssLink) {
        DFSNode node1 = nodes.get(siteLink1);
        DFSNode node2 = nodes.get(siteLink2);
        if (hasCommonFreq(node1, node2)) {
            node1.addNeighbor(siteLink2, wssLink);
            node2.addNeighbor(siteLink1, wssLink);
        }
    }

    private Boolean hasCommonFreq(DFSNode node1, DFSNode node2) {
        List<Long> commonFrequenciesTest = new ArrayList<>(node1.frequencies);
        commonFrequenciesTest.retainAll(node2.frequencies);
        if (commonFrequenciesTest.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<DFSPath> findAllPaths(String start, String end) {
        //Calculate commonFrequencies between start and end, as the init commonFrequencies.
        DFSNode node1 = nodes.get(start);
        DFSNode node2 = nodes.get(end);
        List<Long> commonFrequencies = new ArrayList<>(node1.frequencies);
        commonFrequencies.retainAll(node2.frequencies);
        if (commonFrequencies.isEmpty()) {
            return Collections.EMPTY_LIST;//no path
        }

        HashSet<String> visited = new HashSet<>();
        List<DFSPath> result = new ArrayList<>();
        List<String> path = new ArrayList<>();
        findAllPaths(start, end, visited, path, commonFrequencies, result);
        return result;
    }

    private void findAllPaths(String start, String end, Set<String> visited,
            List<String> path, List<Long> commonFrequencies, List<DFSPath> result) {

        DFSNode endNode = nodes.get(end);
        commonFrequencies.retainAll(endNode.frequencies);
        if (commonFrequencies.isEmpty()) {
            return;//no path
        }

        path.add(start);
        visited.add(start);

        if (start == end) {
            result.add(new DFSPath(new ArrayList<>(path), commonFrequencies));
            return;
        }

        DFSNode currentNode = nodes.get(start);
        // Continue DFS to search for end DFSNode
        for (Entry<String, String> neighborEntry : currentNode.neighborEdges.entrySet()) {
            String siteLink = neighborEntry.getKey();
            String wssLink = neighborEntry.getValue();
            if (!visited.contains(siteLink)) {
                DFSNode siteLinkNode = nodes.get(siteLink);
                List<Long> pathCommonFrequencies = new ArrayList<>(commonFrequencies);
                pathCommonFrequencies.retainAll(siteLinkNode.frequencies);
                if (pathCommonFrequencies.isEmpty()) {
                    continue;
                }
                path.add(wssLink);
                findAllPaths(siteLink, end, new HashSet<>(visited), new ArrayList<>(path), pathCommonFrequencies, result);
                path.remove(path.size() - 1);
            }
        }
    }


}
