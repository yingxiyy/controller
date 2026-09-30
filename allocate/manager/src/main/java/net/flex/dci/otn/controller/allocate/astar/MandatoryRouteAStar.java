/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.astar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import net.flex.dci.otn.controller.allocate.Dijkstra.DJNode;
import net.flex.dci.otn.controller.allocate.Dijkstra.Graph;

/**
 * Finds the shortest simple SiteLink route that covers unordered mandatory resources.
 *
 * <p>The graph and its WSS adjacency are still built by {@code TunnelComputer2}; this
 * class only changes the search strategy. A search is run for one center frequency so
 * every SiteLink in the returned path is guaranteed to carry that same frequency.</p>
 */
public final class MandatoryRouteAStar {

    private MandatoryRouteAStar() {
    }

    public static List<DJNode> findPath(Graph graph, DJNode source, DJNode destination,
                                        String sourceSiteId, String destinationSiteId, long centerFrequency,
                                        Set<String> mandatorySiteIds, Set<String> mandatorySiteLinkIds,
                                        Map<String, Set<String>> mandatorySiteLinkCoverage,
                                        Set<String> excludedSiteIds, Set<String> excludedSiteLinkIds) {
        if (graph == null || source == null || destination == null
                || !supportsFrequency(source, centerFrequency)
                || !supportsFrequency(destination, centerFrequency)
                || excludedSiteLinkIds.contains(source.getSiteLinkId())
                || excludedSiteLinkIds.contains(destination.getSiteLinkId())) {
            return Collections.emptyList();
        }

        ResourceMask resourceMask = new ResourceMask(
                mandatorySiteIds, mandatorySiteLinkIds, mandatorySiteLinkCoverage);
        Map<String, Integer> distanceToDestination = buildDistanceToDestination(
                graph, destination, centerFrequency, excludedSiteLinkIds);
        if (!source.getSiteLinkId().equals(destination.getSiteLinkId())
                && !distanceToDestination.containsKey(source.getSiteLinkId())) {
            return Collections.emptyList();
        }

        String firstReachedSite = otherSite(source, sourceSiteId);
        if (firstReachedSite == null || isExcludedIntermediateSite(firstReachedSite,
                sourceSiteId, destinationSiteId, excludedSiteIds)) {
            return Collections.emptyList();
        }

        Set<String> visitedSites = new HashSet<>();
        visitedSites.add(sourceSiteId);
        if (!visitedSites.add(firstReachedSite)) {
            return Collections.emptyList();
        }
        BitSet coveredResources = resourceMask.coveredBy(source.getSiteLinkId(),
                sourceSiteId, firstReachedSite);
        List<DJNode> initialPath = new ArrayList<>();
        initialPath.add(source);
        State initial = new State(source, firstReachedSite, initialPath, visitedSites,
                coveredResources, 1, heuristic(distanceToDestination, source));

        PriorityQueue<State> open = new PriorityQueue<>(Comparator
                .comparingInt(State::estimatedCost)
                .thenComparingInt(state -> state.cost));
        Map<StateKey, Integer> bestCosts = new HashMap<>();
        open.add(initial);
        bestCosts.put(initial.key(), initial.cost);

        while (!open.isEmpty()) {
            State current = open.poll();
            Integer bestKnownCost = bestCosts.get(current.key());
            if (bestKnownCost != null && current.cost > bestKnownCost) {
                continue;
            }
            if (current.node.getSiteLinkId().equals(destination.getSiteLinkId())
                    && current.currentSiteId.equals(destinationSiteId)
                    && resourceMask.isComplete(current.coveredResources)) {
                return current.path;
            }

            for (DJNode adjacent : current.node.getAdjacentNodes().keySet()) {
                if (!supportsFrequency(adjacent, centerFrequency)
                        || excludedSiteLinkIds.contains(adjacent.getSiteLinkId())) {
                    continue;
                }
                String nextSiteId = otherSite(adjacent, current.currentSiteId);
                if (nextSiteId == null
                        || current.visitedSiteIds.contains(nextSiteId)
                        || isExcludedIntermediateSite(nextSiteId, sourceSiteId,
                        destinationSiteId, excludedSiteIds)) {
                    continue;
                }
                Integer remainingDistance = distanceToDestination.get(adjacent.getSiteLinkId());
                if (remainingDistance == null
                        && !adjacent.getSiteLinkId().equals(destination.getSiteLinkId())) {
                    continue;
                }

                List<DJNode> nextPath = new ArrayList<>(current.path);
                nextPath.add(adjacent);
                Set<String> nextVisitedSites = new HashSet<>(current.visitedSiteIds);
                nextVisitedSites.add(nextSiteId);
                BitSet nextCoveredResources = (BitSet) current.coveredResources.clone();
                resourceMask.mark(nextCoveredResources, adjacent.getSiteLinkId(),
                        current.currentSiteId, nextSiteId);
                State next = new State(adjacent, nextSiteId, nextPath, nextVisitedSites,
                        nextCoveredResources, current.cost + 1,
                        remainingDistance == null ? 0 : remainingDistance);
                StateKey key = next.key();
                Integer previousCost = bestCosts.get(key);
                if (previousCost == null || next.cost < previousCost) {
                    bestCosts.put(key, next.cost);
                    open.add(next);
                }
            }
        }
        return Collections.emptyList();
    }

    private static Map<String, Integer> buildDistanceToDestination(Graph graph, DJNode destination,
                                                                    long centerFrequency,
                                                                    Set<String> excludedSiteLinkIds) {
        Map<String, Integer> distances = new HashMap<>();
        if (excludedSiteLinkIds.contains(destination.getSiteLinkId())) {
            return distances;
        }
        Queue<DJNode> queue = new ArrayDeque<>();
        distances.put(destination.getSiteLinkId(), 0);
        queue.add(destination);
        while (!queue.isEmpty()) {
            DJNode current = queue.remove();
            int nextDistance = distances.get(current.getSiteLinkId()) + 1;
            for (DJNode adjacent : current.getAdjacentNodes().keySet()) {
                if (!supportsFrequency(adjacent, centerFrequency)
                        || excludedSiteLinkIds.contains(adjacent.getSiteLinkId())
                        || distances.containsKey(adjacent.getSiteLinkId())) {
                    continue;
                }
                distances.put(adjacent.getSiteLinkId(), nextDistance);
                queue.add(adjacent);
            }
        }
        return distances;
    }

    private static int heuristic(Map<String, Integer> distances, DJNode node) {
        Integer distance = distances.get(node.getSiteLinkId());
        return distance == null ? 0 : distance;
    }

    private static boolean supportsFrequency(DJNode node, long centerFrequency) {
        return node.getAvailableCentFrequency() != null
                && node.getAvailableCentFrequency().contains(centerFrequency);
    }

    private static boolean isExcludedIntermediateSite(String siteId, String sourceSiteId,
                                                       String destinationSiteId,
                                                       Set<String> excludedSiteIds) {
        return !siteId.equals(sourceSiteId)
                && !siteId.equals(destinationSiteId)
                && excludedSiteIds.contains(siteId);
    }

    private static String otherSite(DJNode node, String currentSiteId) {
        if (node.getSiteA().equals(currentSiteId)) {
            return node.getSiteZ();
        }
        if (node.getSiteZ().equals(currentSiteId)) {
            return node.getSiteA();
        }
        return null;
    }

    private static final class State {
        private final DJNode node;
        private final String currentSiteId;
        private final List<DJNode> path;
        private final Set<String> visitedSiteIds;
        private final BitSet coveredResources;
        private final int cost;
        private final int heuristic;

        private State(DJNode node, String currentSiteId, List<DJNode> path,
                      Set<String> visitedSiteIds, BitSet coveredResources,
                      int cost, int heuristic) {
            this.node = node;
            this.currentSiteId = currentSiteId;
            this.path = path;
            this.visitedSiteIds = visitedSiteIds;
            this.coveredResources = coveredResources;
            this.cost = cost;
            this.heuristic = heuristic;
        }

        private int estimatedCost() {
            return cost + heuristic;
        }

        private StateKey key() {
            return new StateKey(node.getSiteLinkId(), currentSiteId,
                    visitedSiteIds, coveredResources);
        }
    }

    /**
     * The visited-site set is part of the state identity because two arrivals at the
     * same SiteLink can have different legal continuations when simple paths are required.
     */
    private static final class StateKey {
        private final String siteLinkId;
        private final String currentSiteId;
        private final Set<String> visitedSiteIds;
        private final BitSet coveredResources;

        private StateKey(String siteLinkId, String currentSiteId, Set<String> visitedSiteIds,
                         BitSet coveredResources) {
            this.siteLinkId = siteLinkId;
            this.currentSiteId = currentSiteId;
            this.visitedSiteIds = new HashSet<>(visitedSiteIds);
            this.coveredResources = (BitSet) coveredResources.clone();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StateKey)) {
                return false;
            }
            StateKey other = (StateKey) obj;
            return siteLinkId.equals(other.siteLinkId)
                    && currentSiteId.equals(other.currentSiteId)
                    && visitedSiteIds.equals(other.visitedSiteIds)
                    && coveredResources.equals(other.coveredResources);
        }

        @Override
        public int hashCode() {
            int result = siteLinkId.hashCode();
            result = 31 * result + currentSiteId.hashCode();
            result = 31 * result + visitedSiteIds.hashCode();
            result = 31 * result + coveredResources.hashCode();
            return result;
        }
    }

    private static final class ResourceMask {
        private final Map<String, Integer> siteBits = new HashMap<>();
        private final Map<String, Integer> siteLinkBits = new HashMap<>();
        private final Map<String, Set<String>> siteLinkCoverage;
        private final int resourceCount;

        private ResourceMask(Set<String> mandatorySiteIds, Set<String> mandatorySiteLinkIds,
                             Map<String, Set<String>> siteLinkCoverage) {
            List<String> sites = new ArrayList<>(mandatorySiteIds);
            List<String> siteLinks = new ArrayList<>(mandatorySiteLinkIds);
            Collections.sort(sites);
            Collections.sort(siteLinks);
            int bit = 0;
            for (String site : sites) {
                siteBits.put(site, bit++);
            }
            for (String siteLink : siteLinks) {
                siteLinkBits.put(siteLink, bit++);
            }
            this.siteLinkCoverage = siteLinkCoverage;
            resourceCount = bit;
        }

        private BitSet coveredBy(String siteLinkId, String firstSiteId, String secondSiteId) {
            BitSet covered = new BitSet(resourceCount);
            mark(covered, siteLinkId, firstSiteId, secondSiteId);
            return covered;
        }

        private void mark(BitSet covered, String siteLinkId, String firstSiteId,
                          String secondSiteId) {
            mark(covered, siteBits.get(firstSiteId));
            mark(covered, siteBits.get(secondSiteId));
            mark(covered, siteLinkBits.get(siteLinkId));
            for (Map.Entry<String, Set<String>> coverage : siteLinkCoverage.entrySet()) {
                if (coverage.getValue().contains(siteLinkId)) {
                    mark(covered, siteBits.get(coverage.getKey()));
                }
            }
        }

        private void mark(BitSet covered, Integer bit) {
            if (bit != null) {
                covered.set(bit);
            }
        }

        private boolean isComplete(BitSet covered) {
            return covered.cardinality() == resourceCount;
        }
    }
}
