package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;

/**
 * Merges cross connections in route traversal order after node insertion and rebuilds sequence/key.
 * Prefers TP-based ordering; otherwise, inserts the new group within neighboring node XC boundaries.
 * Expands only the affected SiteLink for OCH ordering, without requiring complete routes of other SiteLinks.
 */
final class RouteCrossConnectionOrder {
    private RouteCrossConnectionOrder() {
    }

    /**
     * Merges by XC ID, replacing old entries with new ones, then sorts by the earliest endpoint on the route.
     * routeTps must follow the current branch's traversal direction; unlocatable XCs cause an ordering exception.
     */
    static List<CrossConnections> merge(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<String> routeTps) {
        // Preserve first-occurrence order so stable sorting keeps XCs at the same position in their original order.
        Map<String, CrossConnections> merged = new LinkedHashMap<>();
        if (oldXcs != null) {
            for (CrossConnections xc : oldXcs) {
                merged.put(xc.getCrossConnectionId().getValue(), xc);
            }
        }
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections xc : addedXcs) {
            merged.put(xc.getCrossConnectionId().getValue(), new CrossConnectionsBuilder(xc).build());
        }
        List<CrossConnections> result = new ArrayList<>(merged.values());
        Map<CrossConnections, Integer> positions = new HashMap<>();
        for (CrossConnections xc : result) {
            int position = position(xc, routeTps);
            if (position < 0) {
                throw invalid("cannot locate cross connection in route: " + xc.getCrossConnectionId().getValue());
            }
            positions.put(xc, position);
        }
        result.sort(Comparator.comparingInt(positions::get));
        return reindex(result);
    }

    /** Merges SiteLink XCs, falling back to neighboring boundaries only when normal ordering cannot be resolved. */
    static List<CrossConnections> mergeWithNeighborFallback(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<String> routeTps) {
        try {
            return merge(oldXcs, addedXcs, routeTps);
        } catch (UnresolvedOrderException e) {
            return mergeBetweenNeighbors(oldXcs, addedXcs, Collections.singletonList(routeTps));
        }
    }

    /**
     * Falls back after normal OCH ordering fails, collecting target SiteLink paths through the inserted node.
     * Uses TPs around the target hop for OCH direction, or the supplied from/to for ASE with empty EROs.
     * siteLink must be the updated version whose route already contains the inserted node's TPs.
     * Completes missing local boundaries using outer explicit TPs and adjacent segment membership from relativeSide.
     */
    static List<CrossConnections> mergeOchBetweenNeighbors(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<ExplicitRouteObjects> eros, Link siteLink, String from, String to,
            ToIntFunction<CrossConnections> relativeSide) {
        if (siteLink == null) {
            throw invalid("cannot expand missing siteLink route");
        }
        String insertedNode = insertedNode(addedXcs);
        List<List<String>> candidates = new ArrayList<>();
        boolean foundHop = false;
        for (List<PathRouteObject> path : paths(eros)) {
            for (int i = 0; i < path.size(); i++) {
                if (siteLink.getLinkId().getValue().equals(linkRef(path.get(i)))) {
                    foundHop = true;
                    addNeighborCandidates(candidates, siteLink, insertedNode,
                            i == 0 ? null : tpRef(path.get(i - 1)),
                            i + 1 == path.size() ? null : tpRef(path.get(i + 1)));
                }
            }
        }
        // Use OCH endpoints only for empty paths; paths explicitly traversing other SiteLinks are not target paths.
        if (!foundHop && paths(eros).stream().allMatch(List::isEmpty)) {
            addNeighborCandidates(candidates, siteLink, insertedNode, from, to);
        }
        if (candidates.isEmpty()) {
            throw invalid("cannot locate inserted node in target siteLink route");
        }
        return mergeBetweenNeighbors(oldXcs, addedXcs, candidates,
                outerSideResolver(eros, siteLink.getLinkId().getValue()), relativeSide);
    }

    /**
     * Locates outer XCs using only explicit TPs before and after the target hop, without expanding other SiteLinks.
     * Returns null if the target hop is absent; an XC matching both sides is unknown and cannot define a boundary.
     */
    private static ToIntFunction<CrossConnections> outerSideResolver(List<ExplicitRouteObjects> eros, String targetId) {
        List<String> before = new ArrayList<>();
        List<String> after = new ArrayList<>();
        boolean found = false;
        for (List<PathRouteObject> path : paths(eros)) {
            for (int i = 0; i < path.size(); i++) {
                if (!targetId.equals(linkRef(path.get(i)))) {
                    continue;
                }
                found = true;
                for (int j = 0; j < path.size(); j++) {
                    String tp = tpRef(path.get(j));
                    if (tp != null) {
                        (j < i ? before : after).add(tp);
                    }
                }
            }
        }
        return !found ? null : xc -> {
            List<String> endpoints = endpointTps(xc);
            boolean isBefore = position(endpoints, before, false) >= 0;
            boolean isAfter = position(endpoints, after, false) >= 0;
            return isBefore == isAfter ? 0 : isBefore ? -1 : 1;
        };
    }

    /**
     * Preserves known local boundaries and uses outer information to narrow missing bounds, possibly to either list end.
     * Returns only a unique insertion position; unknown outer XCs must not be skipped when resolving the interval.
     */
    private static int[] completeOchBoundaries(List<CrossConnections> existing, int before, int after,
            ToIntFunction<CrossConnections> outerSide, ToIntFunction<CrossConnections> relativeSide) {
        int lower = Math.max(0, before);
        int upper = after < 0 ? existing.size() : after;
        int[] sides = new int[existing.size()];
        for (int i = 0; i < existing.size(); i++) {
            sides[i] = outerSide.applyAsInt(existing.get(i));
            if (sides[i] < 0) {
                lower = Math.max(lower, i + 1);
            } else if (sides[i] > 0) {
                upper = Math.min(upper, i);
            }
        }
        // Do not read other SiteLinks when explicit TPs already resolve the position, avoiding extra data dependencies.
        int firstUnknown = lower;
        int lastUnknown = upper;
        for (int i = firstUnknown; i < lastUnknown; i++) {
            int side = sides[i] == 0 ? relativeSide.applyAsInt(existing.get(i)) : sides[i];
            if (side < 0) {
                lower = Math.max(lower, i + 1);
            } else if (side > 0) {
                upper = Math.min(upper, i);
            }
        }
        if (lower != upper) {
            throw invalid("cannot determine inserted node boundaries from outer OCH route");
        }
        return new int[] {lower, upper};
    }

    /** Keeps all branches through the inserted node in OCH traversal order, without arbitrarily selecting a branch. */
    private static void addNeighborCandidates(List<List<String>> result, Link siteLink,
            String insertedNode, String from, String to) {
        boolean found = false;
        for (List<String> candidate : siteCandidates(siteLink)) {
            if (candidate.stream().noneMatch(tp -> insertedNode.equals(nodeId(tp)))) {
                continue;
            }
            // Try TP, board, then node matching; relaxed matching determines direction, not a unique protection branch.
            int direction = 0;
            for (int level = 0; level < 3 && direction == 0; level++) {
                boolean forward = matchesEndpoint(from, candidate.get(0), level)
                        && matchesEndpoint(to, candidate.get(candidate.size() - 1), level);
                boolean reverse = matchesEndpoint(to, candidate.get(0), level)
                        && matchesEndpoint(from, candidate.get(candidate.size() - 1), level);
                if (forward && reverse) {
                    throw invalid("ambiguous target siteLink traversal direction");
                }
                direction = forward ? 1 : reverse ? -1 : 0;
            }
            if (direction == 0) {
                throw invalid("cannot orient affected siteLink branch for neighbor insertion");
            }
            List<String> oriented = new ArrayList<>(candidate);
            if (direction < 0) {
                Collections.reverse(oriented);
            }
            addDistinct(result, oriented);
            found = true;
        }
        if (!found) {
            throw invalid("cannot find affected siteLink branch for neighbor insertion");
        }
    }

    /** Direction matching levels: 0 for the same TP, 1 for the same board, and 2 for the same physical node. */
    private static boolean matchesEndpoint(String first, String second, int level) {
        return first != null && (level == 0 ? first.equals(second)
                : level == 1 ? sameEquipment(first, second) : nodeId(first).equals(nodeId(second)));
    }

    /** Extracts the physical node ID, keeping different NEs within the same site distinct. */
    private static String nodeId(String tp) {
        return PhysicalTpIdNamingRule.getNodeId(tp);
    }

    /** Ensures all new XC endpoints belong to one node so the fallback can insert them as a single group. */
    private static String insertedNode(List<? extends CrossConnectionAttributes> addedXcs) {
        String node = null;
        for (CrossConnectionAttributes xc : addedXcs) {
            List<String> endpoints = endpointTps(xc);
            if (endpoints.isEmpty()) {
                throw invalid("inserted cross connection has no endpoints");
            }
            for (String tp : endpoints) {
                String current = nodeId(tp);
                if (node != null && !node.equals(current)) {
                    throw invalid("inserted cross connections belong to different nodes");
                }
                node = current;
            }
        }
        if (node == null) {
            throw invalid("no inserted cross connections for neighbor insertion");
        }
        return node;
    }

    /**
     * Inserts the new XC group within the interval allowed by all candidate paths, without reordering existing XCs.
     * Prefers route ordering within the group; keeps generation order only if a candidate fails or orders disagree.
     * candidates must share a traversal direction; without outer information, both boundaries must be found locally.
     */
    private static List<CrossConnections> mergeBetweenNeighbors(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<List<String>> candidates) {
        return mergeBetweenNeighbors(oldXcs, addedXcs, candidates, null, xc -> 0);
    }

    /** Adds outer OCH boundary support to group insertion, using outer information only when local bounds are missing. */
    private static List<CrossConnections> mergeBetweenNeighbors(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<List<String>> candidates, ToIntFunction<CrossConnections> outerSide,
            ToIntFunction<CrossConnections> relativeSide) {
        String node = insertedNode(addedXcs);
        Map<String, CrossConnections> added = new LinkedHashMap<>();
        addedXcs.forEach(xc -> added.put(xc.getCrossConnectionId().getValue(), new CrossConnectionsBuilder(xc).build()));
        List<CrossConnections> existing = new ArrayList<>();
        // Remove old entries with matching IDs before reinserting the group to avoid duplicates on repeated execution.
        if (oldXcs != null) {
            oldXcs.stream().filter(xc -> !added.containsKey(xc.getCrossConnectionId().getValue())).forEach(existing::add);
        }
        // lower/upper bound an inclusive interval of insertion positions, not XC indices; size means append.
        int lower = 0;
        int upper = existing.size();
        List<CrossConnections> ordered = null;
        boolean precise = true;
        boolean needsOuterBounds = false;
        for (List<String> candidate : candidates) {
            int first = -1;
            int last = -1;
            for (int i = 0; i < candidate.size(); i++) {
                if (node.equals(nodeId(candidate.get(i)))) {
                    if (first < 0) {
                        first = i;
                    }
                    last = i;
                }
            }
            if (first < 0) {
                throw invalid("cannot locate inserted node in route");
            }
            // The inserted node must occupy a contiguous route segment to be represented by a single boundary pair.
            for (int i = first; i <= last; i++) {
                if (!node.equals(nodeId(candidate.get(i)))) {
                    throw invalid("inserted node occurs in multiple route segments");
                }
            }
            if (!existing.isEmpty()) {
                int before = neighborBoundary(existing, candidate, first - 1, -1);
                int after = neighborBoundary(existing, candidate, last + 1, 1);
                if (before < 0 || after < 0) {
                    if (outerSide == null) {
                        throw invalid("cannot locate cross connections on both sides of inserted node");
                    }
                    needsOuterBounds = true;
                }
                // Intersect protection branch intervals so the final position satisfies every branch's boundaries.
                lower = Math.max(lower, Math.max(0, before));
                upper = Math.min(upper, after < 0 ? existing.size() : after);
            }
            // An ambiguous full path may still allow ordering within the inserted node, so try precise sorting first.
            try {
                List<CrossConnections> sorted = merge(Collections.emptyList(), addedXcs, candidate);
                if (ordered != null && !ordered.equals(sorted)) {
                    precise = false;
                }
                ordered = sorted;
            } catch (UnresolvedOrderException e) {
                precise = false;
            }
        }
        if (lower > upper || candidates.isEmpty()) {
            throw invalid("conflicting cross connection boundaries for inserted node");
        }
        // Combine known bounds from all branches before using outer information; one missing anchor need not fail early.
        if (needsOuterBounds) {
            int[] bounds = completeOchBoundaries(existing, lower, upper, outerSide, relativeSide);
            lower = bounds[0];
            upper = bounds[1];
        }
        // Any position in the intersection satisfies the bounds; choose lower and preserve existing XC relative order.
        existing.addAll(lower, precise && ordered != null ? ordered : new ArrayList<>(added.values()));
        return reindex(existing);
    }

    /**
     * Searches outward from the insertion point for the nearest node with existing XCs, skipping nodes without XCs.
     * step=-1 returns the position after the preceding node's XC group; step=1 returns the position before the next group.
     * Returns -1 if none is found; refuses to guess when a neighboring node's XCs form multiple separate groups.
     */
    private static int neighborBoundary(List<CrossConnections> existing, List<String> routeTps,
            int start, int step) {
        for (int tpIndex = start; tpIndex >= 0 && tpIndex < routeTps.size(); tpIndex += step) {
            String node = nodeId(routeTps.get(tpIndex));
            int boundary = -1;
            int lastMatch = -1;
            for (int i = 0; i < existing.size(); i++) {
                if (endpointTps(existing.get(i)).stream().anyMatch(tp -> node.equals(nodeId(tp)))) {
                    if (lastMatch >= 0 && lastMatch + 1 != i) {
                        throw invalid("neighbor cross connections occur in multiple groups: " + node);
                    }
                // Use the position after the last preceding XC or before the first following XC, never inside a group.
                    boundary = step < 0 ? i + 1 : boundary < 0 ? i : boundary;
                    lastMatch = i;
                }
            }
            if (boundary >= 0) {
                return boundary;
            }
        }
        return -1;
    }

    /** Merges OCH XCs without external segment membership, reporting an ordering exception if placement is unresolved. */
    static List<CrossConnections> mergeOch(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<String> targetTps, List<String> outerTps) {
        return mergeOch(oldXcs, addedXcs, targetTps, outerTps, xc -> 0);
    }

    /**
     * Merges and sorts only target SiteLink XCs, preserving the relative order of XCs in other segments.
     * targetTps contains ordered TPs expanded from the target segment; outerTps contains TPs listed directly in OCH EROs.
     * Uses relativeSide when no old target XC provides an anchor: -1 means before the target, 1 after, and 0 unknown.
     */
    static List<CrossConnections> mergeOch(List<CrossConnections> oldXcs,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> addedXcs,
            List<String> targetTps, List<String> outerTps, ToIntFunction<CrossConnections> relativeSide) {
        List<CrossConnections> existing = oldXcs == null ? Collections.emptyList() : oldXcs;
        List<CrossConnections> targetXcs = new ArrayList<>();
        int firstTarget = -1;
        for (int i = 0; i < existing.size(); i++) {
            CrossConnections xc = existing.get(i);
            // Membership requires exact port matches; sharing a board does not make an external XC part of the target.
            if (position(endpointTps(xc), targetTps, false) >= 0) {
                if (firstTarget < 0) {
                    firstTarget = i;
                }
                targetXcs.add(xc);
            }
        }
        List<CrossConnections> ordered = merge(targetXcs, addedXcs, targetTps);
        if (existing.isEmpty()) {
            return ordered;
        }
        if (firstTarget < 0) {
            int insertion = insertionWithoutTargetXcs(existing, targetTps, outerTps, relativeSide);
            List<CrossConnections> result = new ArrayList<>(existing);
            result.addAll(insertion, ordered);
            return reindex(result);
        }
        // Place the sorted group at the first old target XC; this does not repair a historically misplaced whole group.
        List<CrossConnections> result = new ArrayList<>();
        for (int i = 0; i < existing.size(); i++) {
            if (i == firstTarget) {
                result.addAll(ordered);
            }
            if (!targetXcs.contains(existing.get(i))) {
                result.add(existing.get(i));
            }
        }
        return reindex(result);
    }

    /** Resolves a unique insertion position from explicit OCH TPs and external membership when no old target XC exists. */
    private static int insertionWithoutTargetXcs(List<CrossConnections> existing,
            List<String> targetTps, List<String> outerTps, ToIntFunction<CrossConnections> relativeSide) {
        if (targetTps.isEmpty()) {
            throw invalid("cannot locate target siteLink in OCH route");
        }
        int start = tpPosition(targetTps.get(0), outerTps);
        int end = tpPosition(targetTps.get(targetTps.size() - 1), outerTps);
        if (start < 0 || end < start) {
            throw invalid("cannot locate target siteLink endpoints in OCH route");
        }
        int lower = 0;
        int upper = existing.size();
        for (int i = 0; i < existing.size(); i++) {
            int position = position(endpointTps(existing.get(i)), outerTps, false);
            if (position >= 0 && position < start) {
                lower = i + 1;
            } else if (position > end) {
                upper = Math.min(upper, i);
            }
        }
        // Query membership only within the unresolved interval, without expanding or orienting other SiteLink routes.
        if (lower < upper) {
            int firstUnknown = lower;
            int lastUnknown = upper;
            for (int i = firstUnknown; i < lastUnknown; i++) {
                int side = relativeSide.applyAsInt(existing.get(i));
                if (side < 0) {
                    lower = i + 1;
                } else if (side > 0) {
                    upper = Math.min(upper, i);
                }
            }
        }
        // Unlike neighbor fallback, physical bounds are not established here; a wider interval cannot be chosen freely.
        if (lower != upper) {
            throw invalid("cannot determine target siteLink insertion position from existing OCH cross connections");
        }
        return lower;
    }

    /** Finds a TP's route position by exact port match, then board match; returns -1 if neither matches. */
    private static int tpPosition(String tp, List<String> routeTps) {
        List<String> endpoint = Collections.singletonList(tp);
        int position = position(endpoint, routeTps, false);
        return position < 0 ? position(endpoint, routeTps, true) : position;
    }

    /** Rebuilds sequence and YANG list key from 1 in list order, replacing entries without mutating original XC objects. */
    private static List<CrossConnections> reindex(List<CrossConnections> result) {
        for (int i = 0; i < result.size(); i++) {
            long sequence = i + 1L;
            result.set(i, new CrossConnectionsBuilder(result.get(i))
                    .setSequence(sequence).setKey(new CrossConnectionsKey(sequence)).build());
        }
        return result;
    }

    /** Ranks an XC by its earliest endpoint on the route, independently of its source/destination direction. */
    private static int position(CrossConnections xc, List<String> routeTps) {
        List<String> endpoints = endpointTps(xc);
        int position = position(endpoints, routeTps, false);
        // Board matching only estimates ordering position; it does not establish membership in the target SiteLink.
        return position < 0 ? position(endpoints, routeTps, true) : position;
    }

    /** Collects all source and destination TPs, supporting one-to-many endpoints and either route traversal direction. */
    private static List<String> endpointTps(CrossConnectionAttributes xc) {
        List<String> endpoints = new ArrayList<>();
        if (xc.getSourceTp() != null) {
            xc.getSourceTp().forEach(tp -> endpoints.add(tp.getTpRef().getValue()));
        }
        if (xc.getDestinationTp() != null) {
            xc.getDestinationTp().forEach(tp -> endpoints.add(tp.getTpRef().getValue()));
        }
        return endpoints;
    }

    /** Collects new XC endpoints to filter SiteLink branches through the insertion point; the result is not route-ordered. */
    static List<String> insertedTps(List<? extends CrossConnectionAttributes> xcs) {
        List<String> result = new ArrayList<>();
        xcs.forEach(xc -> result.addAll(endpointTps(xc)));
        return result;
    }

    /** Finds the first route match; equipmentOnly selects board matching, otherwise an exact TP match is required. */
    private static int position(List<String> endpoints, List<String> routeTps, boolean equipmentOnly) {
        for (int i = 0; i < routeTps.size(); i++) {
            for (String endpoint : endpoints) {
                if (equipmentOnly ? sameEquipment(endpoint, routeTps.get(i)) : endpoint.equals(routeTps.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Extracts directly listed TPs from included EROs in index order within each ERO, without expanding Link hops. */
    static List<String> physicalTps(List<ExplicitRouteObjects> eros) {
        List<String> result = new ArrayList<>();
        for (List<PathRouteObject> path : paths(eros)) {
            for (PathRouteObject object : path) {
                String tp = tpRef(object);
                if (tp != null) {
                    result.add(tp);
                }
            }
        }
        return result;
    }

    /**
     * Expands only the target SiteLink hop in OCH, using the TPs on either side to determine traversal direction.
     * Does not read other SiteLinks; callers decide whether to use neighbor fallback when expansion is ambiguous.
     */
    static List<String> ochTps(List<ExplicitRouteObjects> eros, Link targetSiteLink, List<String> insertedTps) {
        List<String> result = new ArrayList<>();
        for (List<PathRouteObject> path : paths(eros)) {
            for (int i = 0; i < path.size(); i++) {
                PathRouteObject object = path.get(i);
                if (object.getResourceType() instanceof
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link hop =
                            (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) object.getResourceType();
                    String id = hop.getLinkHop().getLinkRef().getValue();
                    if (targetSiteLink.getLinkId().getValue().equals(id)) {
                        String before = i == 0 ? null : tpRef(path.get(i - 1));
                        String after = i + 1 == path.size() ? null : tpRef(path.get(i + 1));
                        result.addAll(siteTps(targetSiteLink, before, after, insertedTps));
                    }
                }
            }
        }
        return result;
    }

    /** Selects and orients a unique SiteLink path using endpoint TPs only, without filtering by inserted ports. */
    static List<String> siteTps(Link siteLink, String from, String to) {
        return siteTps(siteLink, from, to, Collections.emptyList());
    }

    /**
     * Filters branches containing all inserted ports and orients them from {@code from} to {@code to},
     * requiring one distinct TP sequence.
     * Different paths remain ambiguous even if all pass the insertion point; callers handle them with neighbor fallback.
     */
    static List<String> siteTps(Link siteLink, String from, String to, List<String> insertedTps) {
        List<List<String>> candidates = siteCandidates(siteLink);
        // Try exact endpoint TPs first, then board matching to accommodate port differences in OCH channels, MPO, and ASE.
        for (boolean equipmentOnly : new boolean[] {false, true}) {
            List<List<String>> matches = new ArrayList<>();
            for (List<String> candidate : candidates) {
                if (candidate.isEmpty() || !candidate.containsAll(insertedTps)) {
                    continue;
                }
                String first = candidate.get(0);
                String last = candidate.get(candidate.size() - 1);
                if (matches(from, first, equipmentOnly) && matches(to, last, equipmentOnly)) {
                    addDistinct(matches, candidate);
                }
                if (matches(from, last, equipmentOnly) && matches(to, first, equipmentOnly)) {
                    List<String> reversed = new ArrayList<>(candidate);
                    Collections.reverse(reversed);
                    addDistinct(matches, reversed);
                }
            }
            if (matches.size() == 1) {
                return matches.get(0);
            }
            if (matches.size() > 1) {
                throw invalid("ambiguous siteLink route: " + siteLink.getLinkId().getValue());
            }
        }
        throw invalid("cannot orient siteLink route: " + siteLink.getLinkId().getValue()
                + ", from=" + from + ", to=" + to);
    }

    /** Collects primary, secondary, and third TP sequences separately, without assuming which protection path is used. */
    private static List<List<String>> siteCandidates(Link siteLink) {
        if (siteLink == null || siteLink.getAugmentation(Link1.class) == null) {
            throw invalid("cannot expand missing siteLink route");
        }
        ExplictRoute route = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute();
        List<List<String>> candidates = new ArrayList<>();
        if (route != null && route.getRoute() != null) {
            route.getRoute().forEach(branch -> {
                if (branch.getPrimary() != null) {
                    candidates.add(physicalTps(branch.getPrimary().getExplicitRouteObjects()));
                }
                if (branch.getSecondary() != null) {
                    candidates.add(physicalTps(branch.getSecondary().getExplicitRouteObjects()));
                }
                if (branch.getThird() != null) {
                    branch.getThird().forEach(third -> candidates.add(physicalTps(third.getExplicitRouteObjects())));
                }
            });
        }
        return candidates;
    }

    /** Keeps identical TP sequences with the same direction only once, preventing false ambiguity from duplicate branches. */
    private static void addDistinct(List<List<String>> paths, List<String> path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }

    /**
     * Creates a resolver using external SiteLink port references: -1 for before, 1 for after, and 0 for unknown or conflicting.
     * Initially analyzes only hop order; external port references are loaded lazily and cached within this merge.
     */
    static ToIntFunction<CrossConnections> relativeSideResolver(List<ExplicitRouteObjects> eros,
            String targetId, Function<String, Link> siteLinks) {
        Map<String, Integer> sides = new LinkedHashMap<>();
        for (List<PathRouteObject> path : paths(eros)) {
            int targetIndex = -1;
            for (int i = 0; i < path.size(); i++) {
                if (targetId.equals(linkRef(path.get(i)))) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex < 0) {
                continue;
            }
            for (int i = 0; i < path.size(); i++) {
                String id = linkRef(path.get(i));
                if (id != null && !targetId.equals(id) && SiteLinkIdNamingRule.isSiteLink(id)) {
                    int side = i < targetIndex ? -1 : 1;
                    // An external segment occurring on both sides across different paths cannot determine a unique side.
                    sides.merge(id, side, (oldSide, newSide) -> oldSide.equals(newSide) ? oldSide : 0);
                }
            }
        }
        Map<String, List<String>> ports = new HashMap<>();
        return xc -> {
            int matchedSide = 0;
            for (Map.Entry<String, Integer> entry : sides.entrySet()) {
                List<String> references = ports.computeIfAbsent(entry.getKey(),
                        id -> referencedTps(siteLinks.apply(id)));
                if (position(endpointTps(xc), references, false) < 0) {
                    continue;
                }
                // Return unknown if an XC matches segments on both sides, rather than arbitrarily choosing a side.
                if (entry.getValue() == 0 || (matchedSide != 0 && matchedSide != entry.getValue())) {
                    return 0;
                }
                matchedSide = entry.getValue();
            }
            return matchedSide;
        };
    }

    /** Collects link endpoints and all branch port references for membership only, without validating route completeness or direction. */
    private static List<String> referencedTps(Link link) {
        List<String> result = new ArrayList<>();
        if (link == null) {
            return result;
        }
        if (link.getSource() != null && link.getSource().getSourceTp() != null) {
            result.add(link.getSource().getSourceTp().getValue());
        }
        if (link.getDestination() != null && link.getDestination().getDestTp() != null) {
            result.add(link.getDestination().getDestTp().getValue());
        }
        Link1 augmentation = link.getAugmentation(Link1.class);
        if (augmentation == null || augmentation.getSite() == null
                || augmentation.getSite().getExplictRoute() == null
                || augmentation.getSite().getExplictRoute().getRoute() == null) {
            return result;
        }
        augmentation.getSite().getExplictRoute().getRoute().forEach(route -> {
            if (route.getPrimary() != null) {
                addReferences(result, route.getPrimary().getExplicitRouteObjects(), route.getPrimary().getCrossConnections());
            }
            if (route.getSecondary() != null) {
                addReferences(result, route.getSecondary().getExplicitRouteObjects(), route.getSecondary().getCrossConnections());
            }
            if (route.getThird() != null) {
                route.getThird().forEach(third -> addReferences(result, third.getExplicitRouteObjects(), third.getCrossConnections()));
            }
        });
        return result;
    }

    /** Collects ERO and XC port references, not ordering data, so EROs need not be ordered or fully expandable. */
    private static void addReferences(List<String> result, List<ExplicitRouteObjects> eros, List<CrossConnections> xcs) {
        if (eros != null) {
            for (ExplicitRouteObjects ero : eros) {
                if (ero.getPathRouteObject() != null) {
                    ero.getPathRouteObject().forEach(object -> {
                        String tp = tpRef(object);
                        if (tp != null) {
                            result.add(tp);
                        }
                    });
                }
            }
        }
        if (xcs != null) {
            xcs.forEach(xc -> result.addAll(endpointTps(xc)));
        }
    }

    /** Reads a Link hop reference, returning null for other hop types or incomplete references. */
    private static String linkRef(PathRouteObject object) {
        if (object.getResourceType() instanceof
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link hop =
                    (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) object.getResourceType();
            return hop.getLinkHop() == null || hop.getLinkHop().getLinkRef() == null
                    ? null : hop.getLinkHop().getLinkRef().getValue();
        }
        return null;
    }

    /** Matches a candidate path endpoint; a missing endpoint cannot establish direction. */
    private static boolean matches(String endpoint, String routeTp, boolean equipmentOnly) {
        return endpoint != null && (equipmentOnly ? sameEquipment(endpoint, routeTp) : endpoint.equals(routeTp));
    }

    /** Compares full equipment IDs to match the same board, keeping different boards on one node distinct. */
    private static boolean sameEquipment(String first, String second) {
        return PhysicalTpIdNamingRule.getEquipId(first).equals(PhysicalTpIdNamingRule.getEquipId(second));
    }

    /** Keeps EROs with Include or unspecified usage and sorts each path by index, copying lists to preserve original routes. */
    private static List<List<PathRouteObject>> paths(List<ExplicitRouteObjects> eros) {
        List<List<PathRouteObject>> result = new ArrayList<>();
        if (eros != null) {
            for (ExplicitRouteObjects ero : eros) {
                if (ero.getPathRouteObject() != null && (ero.getExplicitRouteUsage() == null
                        || RouteUsageInclude.class.equals(ero.getExplicitRouteUsage()))) {
                    List<PathRouteObject> path = new ArrayList<>(ero.getPathRouteObject());
                    path.sort(Comparator.comparing(PathRouteObject::getIndex));
                    result.add(path);
                }
            }
        }
        return result;
    }

    /** Reads a TP hop's port reference; returns null for other resource types and expects complete TP hop references. */
    private static String tpRef(PathRouteObject object) {
        if (object.getResourceType() instanceof Tp) {
            Tp tp = (Tp) object.getResourceType();
            return tp.getTpHop().getTpRef().getValue();
        }
        return null;
    }

    /** Creates an ordering exception that callers can distinguish from database and other CommonException failures. */
    private static CommonException invalid(String message) {
        return new UnresolvedOrderException(message);
    }

    /** Indicates unresolved ordering or boundaries only; callers catch this type so fallback does not hide other failures. */
    static final class UnresolvedOrderException extends CommonException {
        private UnresolvedOrderException(String message) {
            super(CommonExceptionType.INVALID_PARAMETER, message);
        }
    }
}
