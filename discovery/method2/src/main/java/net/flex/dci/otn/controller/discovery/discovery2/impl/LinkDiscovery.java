package net.flex.dci.otn.controller.discovery.discovery2.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.discovery.common.impl.CrossConnectionMachine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;

@Slf4j
public class LinkDiscovery {

    DiscoveryResource resource;

    public LinkDiscovery(DiscoveryResource resource) {
        this.resource = resource;
    }

    protected void checkEro(List<ExplicitRouteObjects> explicitRouteObjectsList)
            throws CommonException {
        for (ExplicitRouteObjects ero : explicitRouteObjectsList) {
            checkPro(ero.getPathRouteObject());
        }
    }

    private void checkPro(List<PathRouteObject> pathRouteObjectList) throws CommonException {
        for (PathRouteObject pro : pathRouteObjectList) {
            if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Tp.class.getName())) {
                TpHop hop = ((Tp) pro.getResourceType()).getTpHop();
                String routeTpId = hop.getTpRef().getValue();
                String nodeId = PhysicalTpIdNamingRule.getNodeId(routeTpId);
                Node opNode = resource.getOpNode(nodeId);

                if (opNode == null) {
                    String msg = String.format(
                            "the node from route TP hasn't found related device %s", nodeId);
                    log.debug(msg);
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
                }

                boolean found = false;
                for (TerminationPoint tp : opNode.getTerminationPoint()) {
                    if (tp.getTpId().getValue().equals(routeTpId)) {
                        found = true;
                        try {
                            AdminStatus adminState = tp.getAugmentation(TerminationPoint1.class)
                                    .getPhysical().getAdminState();
                            if (adminState.equals(AdminStatus.Down) || adminState.equals(
                                    AdminStatus.Unknown)) {
                                String msg = String.format("the TP status error, %s == %s",
                                        tp.getTpId().getValue(), adminState);
                                log.debug(msg);
                                throw new CommonException(
                                        CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
                            }
                            break;
                        } catch (Exception e) {
                            String msg = String.format("the TP status error, %s == %s",
                                    tp.getTpId().getValue(), e.getMessage());
                            log.debug(msg);
                            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                    msg);
                        }
                    }
                }
                if (!found) {
                    String msg = String.format("hasn't found route TP in device %s", routeTpId);
                    log.debug(msg);
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
                }
            }
        }
    }

    protected void checkXC(List<CrossConnections> crossConnectionsList) throws CommonException {
        for (CrossConnections xc : crossConnectionsList) {
            String routeXcId = xc.getCrossConnectionId().getValue();
            String routeNodeId = xc.getNodeRef().getValue();

            Node opNode = resource.getOpNode(routeNodeId);
            if (opNode == null) {
                String msg = String.format(
                        "the node from route XC hasn't found related device %, cancel related link discovery",
                        routeNodeId);
                log.debug(msg);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
            }

            routeXcId = CrossConnectionMachine.getNeXcId(routeXcId);

            Physical nodePhyAttr = opNode.getAugmentation(Node1.class).getPhysical();
            boolean found = false;
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc : nodePhyAttr.getCrossConnections()) {
                if (neXc.getCrossConnectionId().getValue().equals(routeXcId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String msg = String.format("hasn't found route XC in device %s", routeXcId);
                log.debug(msg);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
            }

        }
    }
}
