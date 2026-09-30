package net.flex.dci.otn.controller.nms.nms.component.amplifier;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otn.controller.nms.nms.dto.link.OtsLinkAmplifierRefCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * 2026/9/23
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class AmplifierEndpointResolver {

    private final PhyLinkDao phyLinkDao;

    public OtsLinkTerminationPointInfo resolveOtsLinkEndpoint(
            String otsLinkId, OtsLinkAmplifierRefCache refCache) {
        Map<String, Node> neMap = refCache.getPhyNodeMap();

        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(otsLinkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(otsLinkId);

        EndpointEnd src = resolveEnd(sourceTpId, neMap);
        EndpointEnd dst = resolveEnd(destTpId, neMap);

        return OtsLinkTerminationPointInfo.builder()
                .edfaSourceTpId(src.clLookupTp)
                .edfaDestTpId(dst.clLookupTp)
                .ramanSourceTpId(src.ramanTp)
                .ramanDestTpId(dst.ramanTp)
                .build();
//        String sourceEqId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
//        String destEqId = PhysicalTpIdNamingRule.getEquipId(destTpId);
//        String sourceNeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
//        String destNeId = PhysicalTpIdNamingRule.getNodeId(destTpId);
//        Node sourceNe = neMap.get(sourceNeId);
//        Node destNe = neMap.get(destNeId);
//        Equipments sourceEquip = sourceNe.getAugmentation(Node1.class).getPhysical().getEquipments()
//                .stream()
//                .filter(eq -> eq.getEquipmentId().equals(sourceEqId)).findAny().get();
//        Equipments destEquip = destNe.getAugmentation(Node1.class).getPhysical().getEquipments()
//                .stream()
//                .filter(eq -> eq.getEquipmentId().equals(destEqId)).findAny().get();
//        EquipType sourceEquipType = sourceEquip.getEquipType();
//        EquipType destEquipType = destEquip.getEquipType();
//        if (sourceEquipType == EquipType.RAMAN) {
//
//        }
//
//        if (destEquipType == EquipType.RAMAN) {
//
//        }
//
//        String sourceEdfaTpId = null;
//        String destEdfaTpId = null;
//        String sourceRamanTpId = null;
//        String destRamanTpId = null;
//
//        return OtsLinkTerminationPointInfo.builder().edfaSourceTpId(sourceEdfaTpId)
//                .edfaDestTpId(destEdfaTpId).ramanSourceTpId(sourceRamanTpId)
//                .ramanDestTpId(destRamanTpId).build();

    }

    private EndpointEnd resolveEnd(String tpId, Map<String, Node> neMap) {
        Equipments eq = findEquip(neMap, tpId);
        if (eq == null) {
            log.warn("tp {} cannot find the card ,lower the level", tpId);
            return new EndpointEnd(tpId, null);
        }
        if (eq.getEquipType() == EquipType.RAMAN) {
            String iraTp = findRelatedIraTp(tpId);
            return new EndpointEnd(iraTp, tpId);
        }
        return new EndpointEnd(tpId, null);
    }

    private String findRelatedIraTp(String tpId) {
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
        List<Link> innerPhyLinks = phyLinkDao.listAllPhyLinksUnderEquip(
                equipId);
        List<Link> iraInnerPhyLink = innerPhyLinks.stream()
                .filter(link -> !link.getLinkId().getValue().contains(tpId))
                .collect(Collectors.toList());
        String phyLinkId = iraInnerPhyLink.get(0).getLinkId().getValue();
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(phyLinkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(phyLinkId);
        return sourceTpId.contains(equipId) ? destTpId : sourceTpId;
    }

    private Equipments findEquip(Map<String, Node> neMap, String tpId) {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node ne = neMap.get(neId);
        if (ne == null || ne.getAugmentation(Node1.class) == null
                || ne.getAugmentation(Node1.class).getPhysical() == null) {
            return null;
        }
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        return ne.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(e -> e.getEquipmentId().equals(eqId)).findFirst().orElse(null);
    }


    @Data
    @Builder
    public static class OtsLinkTerminationPointInfo implements Serializable {

        private String edfaSourceTpId;
        private String edfaDestTpId;
        private String ramanSourceTpId;
        private String ramanDestTpId;
    }


    @Data
    @AllArgsConstructor
    private static class EndpointEnd implements Serializable {

        private String clLookupTp;
        private String ramanTp;
    }
}
