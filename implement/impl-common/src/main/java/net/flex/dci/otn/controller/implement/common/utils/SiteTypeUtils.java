package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SiteTypeUtils {
    public static SiteType getSiteType(Node node, RouteInfo rInfo, List<String> linkAZNodes) {
        String nodeId = node.getNodeId().getValue();

        List<String> xcIds = rInfo.getXcIdList().stream()
                .filter(x -> x.contains(node.getNodeId().getValue()))
                .collect(Collectors.toList());

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        CrossConnections xc = nodeAttr.getCrossConnections().stream().filter(x -> xcIds.contains(x.getCrossConnectionId().getValue()))
                .findAny().orElse(null);

        if (xc == null) {
            log.error("can not find xc for node:{},xcIds:{}", node.getNodeId().getValue(), xcIds);
            return SiteType.OTM; //not execute to here.
        }
        String eqId = PhysicalTpIdNamingRule.getEquipId(xc.getSourceTp().get(0).getTpRef().getValue());
        Equipments eq = nodeAttr.getEquipments().stream()
                .filter(x -> eqId.equals(x.getEquipmentId()))
                .findAny().orElseThrow(()-> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find required EQ: " + eqId));

        if (! eq.getEquipType().equals(EquipType.DGE) && ! eq.getEquipType().equals(EquipType.ILA)
                && ! eq.getEquipType().equals(EquipType.IRA) && ! eq.getEquipType().equals(EquipType.OA)) {

            String dEqId = PhysicalTpIdNamingRule.getEquipId(xc.getDestinationTp().get(0).getTpRef().getValue());
            eq = nodeAttr.getEquipments().stream()
                    .filter(x -> dEqId.equals(x.getEquipmentId()))
                    .findAny().orElseThrow(()-> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find required EQ: " + dEqId));
        }

        log.debug("current equip id is: {}", eq.getEquipmentId());
        if (eq.getEquipType().equals(EquipType.DGE)) {
            return SiteType.DGE;
        } else if (eq.getEquipType().equals(EquipType.ILA)) {
            return SiteType.ILA;
        } else if (eq.getEquipType().equals(EquipType.IRA)) {
            Equipments finalEq = eq;
            List<String> linkIds = rInfo.getPhyLinkIdList().stream()
                    .filter(linkId -> linkId.contains(finalEq.getEquipmentId())).collect(Collectors.toList());
            String expLink = linkIds.stream()
                    .filter(x -> x.contains("EXP"))
                    .findAny().orElse(null);
            if (expLink == null) {
                if (linkAZNodes.stream().anyMatch(siteNodeId -> nodeId.startsWith(siteNodeId))) {
                    log.info("this is A/Z site, this is OTM");
                    return SiteType.OTM;
                } else {
                    log.info("not A/Z site, this is ROADM/REG");
                    return SiteType.ROADM;  //REG similar as ROADM
                }
            } else {
                log.info("include EXP, so this is ROADM");
                return SiteType.ROADM;
            }
        }
        return SiteType.OTM; //not execute to here.
    }
}
