/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.Dijkstra;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

public class Dijkstra {

    public static Graph calculateShortestPathFromSource(Graph graph, DJNode source) {

        source.setDistance(0);

        Set<DJNode> settledNodes = new HashSet<>();
        Set<DJNode> unsettledNodes = new HashSet<>();
        unsettledNodes.add(source);

        while (unsettledNodes.size() != 0) {
            DJNode currentNode = getLowestDistanceNode(unsettledNodes);
            unsettledNodes.remove(currentNode);
            for (Entry<DJNode, Integer> adjacencyPair : currentNode.getAdjacentNodes().entrySet()) {
                DJNode adjacentNode = adjacencyPair.getKey();
                Integer edgeWeigh = adjacencyPair.getValue();

                if (!settledNodes.contains(adjacentNode)) {
                    CalculateMinimumDistance(adjacentNode, edgeWeigh, currentNode);
                    unsettledNodes.add(adjacentNode);
                }
            }
            settledNodes.add(currentNode);
        }
        return graph;
    }

    private static void CalculateMinimumDistance(DJNode evaluationNode, Integer edgeWeigh, DJNode sourceNode) {
        Integer sourceDistance = sourceNode.getDistance();
        if (sourceDistance + edgeWeigh < evaluationNode.getDistance()) {
            evaluationNode.setDistance(sourceDistance + edgeWeigh);
            LinkedList<DJNode> shortestPath = new LinkedList<>(sourceNode.getShortestPath());
            shortestPath.add(sourceNode);
            evaluationNode.setShortestPath(shortestPath);
        }
    }

    private static DJNode getLowestDistanceNode(Set<DJNode> unsettledNodes) {
        DJNode lowestDistanceNode = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (DJNode node : unsettledNodes) {
            int nodeDistance = node.getDistance();
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }
}
