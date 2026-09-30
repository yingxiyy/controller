/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.MandatoryNode;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.MandatorySiteLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RouteRestriction;

/**
 * Normalizes route-restriction index 0/1/2 into primary/secondary/third constraints.
 *
 * <p>The input lists are intentionally converted to sets. Mandatory resource order is
 * not route order; every listed resource only needs to be covered by its own leg.</p>
 */
final class MandatoryRouteConstraints {

    private final Map<Integer, LegConstraint> constraintsByLeg;

    private MandatoryRouteConstraints(Map<Integer, LegConstraint> constraintsByLeg) {
        this.constraintsByLeg = constraintsByLeg;
    }

    static MandatoryRouteConstraints from(List<RouteRestriction> routeRestrictions) {
        Map<Integer, LegConstraint> result = new HashMap<>();
        if (routeRestrictions == null) {
            return new MandatoryRouteConstraints(result);
        }
        for (RouteRestriction restriction : routeRestrictions) {
            if (restriction == null || restriction.getIndex() == null
                    || restriction.getIndex() < 0 || restriction.getIndex() > 2) {
                continue;
            }
            Set<String> siteIds = restriction.getMandatoryNode() == null
                    ? Collections.<String>emptySet()
                    : restriction.getMandatoryNode().stream()
                    .map(MandatoryNode::getNodeId)
                    .filter(value -> value != null && !value.isEmpty())
                    .collect(Collectors.toSet());
            Set<String> siteLinkIds = restriction.getMandatorySiteLink() == null
                    ? Collections.<String>emptySet()
                    : restriction.getMandatorySiteLink().stream()
                    .map(MandatorySiteLink::getLinkId)
                    .filter(value -> value != null && !value.isEmpty())
                    .collect(Collectors.toSet());
            result.put(restriction.getIndex(), new LegConstraint(siteIds, siteLinkIds));
        }
        return new MandatoryRouteConstraints(result);
    }

    boolean hasMandatoryResources() {
        return constraintsByLeg.values().stream().anyMatch(LegConstraint::hasMandatoryResources);
    }

    LegConstraint get(int legIndex) {
        LegConstraint constraint = constraintsByLeg.get(legIndex);
        return constraint == null ? LegConstraint.EMPTY : constraint;
    }

    Set<String> getAllMandatorySiteLinkIds() {
        Set<String> result = new HashSet<>();
        for (LegConstraint constraint : constraintsByLeg.values()) {
            result.addAll(constraint.getSiteLinkIds());
        }
        return result;
    }

    static final class LegConstraint {
        private static final LegConstraint EMPTY = new LegConstraint(
                Collections.<String>emptySet(), Collections.<String>emptySet());

        private final Set<String> siteIds;
        private final Set<String> siteLinkIds;

        private LegConstraint(Set<String> siteIds, Set<String> siteLinkIds) {
            this.siteIds = Collections.unmodifiableSet(new HashSet<>(siteIds));
            this.siteLinkIds = Collections.unmodifiableSet(new HashSet<>(siteLinkIds));
        }

        Set<String> getSiteIds() {
            return siteIds;
        }

        Set<String> getSiteLinkIds() {
            return siteLinkIds;
        }

        boolean hasMandatoryResources() {
            return !siteIds.isEmpty() || !siteLinkIds.isEmpty();
        }
    }
}
