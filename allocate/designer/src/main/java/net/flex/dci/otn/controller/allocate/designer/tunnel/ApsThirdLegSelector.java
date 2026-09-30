package net.flex.dci.otn.controller.allocate.designer.tunnel;

import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Selects the APS destination that is not used by the existing OCH legs. */
final class ApsThirdLegSelector {

    private ApsThirdLegSelector() {
    }

    static String selectUnusedDestination(Collection<String> apsDestinations,
                                          Set<String> usedRouteTps,
                                          String nodeId) throws NeDesignerException {
        List<String> unused = apsDestinations.stream()
                .filter(tpId -> !usedRouteTps.contains(tpId))
                .distinct()
                .collect(Collectors.toList());
        if (unused.size() != 1) {
            throw new NeDesignerException(String.format(
                    "APS XC on node %s must have exactly one unused destination TP, but found %d: %s",
                    nodeId, unused.size(), unused));
        }
        return unused.get(0);
    }
}
