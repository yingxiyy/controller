package net.flex.dci.otc.controller.otdr.components.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.components.ScanPortHelper;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkRefCache;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/4/2023 4:59 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScanPortHelperImpl implements ScanPortHelper {

    private final PhyNodeDao phyNodeDao;

    private final PhyLinkDao phyLinkDao;


    @Override
    public TerminationPoint getTargetPortRelativeOTDRPort(EquipType businessEquipType,
            List<TerminationPoint> scanPorts, TerminationPoint targetPort,
            OtdrPortDirection portDirection) {
        return null;
    }

    @Override
    public OtsLinkTerminationPointInfo resolveOtsLinkEndpoint(String phyLinkId) {
        log.debug("resolve the ots link endpoint,the linkId is:{}", phyLinkId);
        OtsLinkRefCache otsLinkRefCache = buildLinkRefCache(phyLinkId);
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(phyLinkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(phyLinkId);

        EndpointEnd src = resolveEnd(sourceTpId, otsLinkRefCache.getNodeCache());
        EndpointEnd dst = resolveEnd(destTpId, otsLinkRefCache.getNodeCache());

        return OtsLinkTerminationPointInfo.builder()
                .edfaSourceTp(src.clLookupTp)
                .edfaDestTp(dst.clLookupTp)
                .ramanSourceTp(src.ramanTp)
                .ramanDestTp(dst.ramanTp)
                .nodeMap(otsLinkRefCache.getNodeCache())
                .build();
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


    private OtsLinkRefCache buildLinkRefCache(String linId) {
        Set<String> neIds = new HashSet<>();
        neIds.add(PhysicalLinkIdNamingRule.getNodeAId(linId));
        neIds.add(PhysicalLinkIdNamingRule.getNodeZId(linId));
        List<Node> refNes = phyNodeDao.listConfigPhyNodeByIds(neIds);
        Map<String, Node> result = refNes.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));
        return OtsLinkRefCache.builder().nodeCache(result).build();
    }

    @Data
    @AllArgsConstructor
    private class EndpointEnd implements Serializable {

        private String clLookupTp;
        private String ramanTp;
    }

}
