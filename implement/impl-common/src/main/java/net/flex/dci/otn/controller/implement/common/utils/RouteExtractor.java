package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RouteExtractor {
    public static List<CrossConnectionAttributes> extractorXc(List<Route> routeList) {
        List<CrossConnectionAttributes> xcList = new ArrayList<>();

        for (Route route : routeList) {
            xcList.addAll(route.getPrimary().getCrossConnections());

            if (route.getSecondary() != null) {
                xcList.addAll(route.getSecondary().getCrossConnections());
            }
            if (route.getThird() != null) {
                route.getThird().forEach(third->xcList.addAll(third.getCrossConnections()));
            }
        }

        return xcList;
    }

    public static List<String> fetchOchLPort(ChangedObject changedObject, RouteInfo rInfo) {
        log.debug("checking if need to disable OCH related LINE tp {}", rInfo.getTpIdList());

        Set<String> tpList = rInfo.getTpIdList().stream().filter(tpId -> {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = changedObject.getChangedPhyNode(nodeId);

            TerminationPoint tp = node.getTerminationPoint().stream()
                    .filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);

            if (tp == null) {
                log.error("Cannot find required TP in node {}", tpId);
                return false;
            }

            if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() != null) {
                return true;
            }
            return false;
        }).collect(Collectors.toSet());

        log.debug("OchLink related TPs {}", tpList);
        return new ArrayList<>(tpList);
    }

}
