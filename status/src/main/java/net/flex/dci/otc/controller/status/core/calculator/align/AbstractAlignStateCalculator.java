package net.flex.dci.otc.controller.status.core.calculator.align;

import static net.flex.dci.otc.controller.status.util.Constants.VIRTUAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;

/**
 * @version 1.0
 * @date 6/20/2025 1:56 PM
 */
@Slf4j
public abstract class AbstractAlignStateCalculator<T, V> implements IAlignStateCalculator<T, V> {

    protected final TerminationPointDao terminationPointDao;

    protected final EquipmentsDao equipmentsDao;

    protected final NodeCacheManager nodeCacheManager;

    protected final PhyNodeDao phyNodeDao;

    protected AbstractAlignStateCalculator(TerminationPointDao terminationPointDao,
            EquipmentsDao equipmentsDao, NodeCacheManager nodeCacheManager,
            PhyNodeDao phyNodeDao1) {
        this.terminationPointDao = terminationPointDao;
        this.equipmentsDao = equipmentsDao;
        this.nodeCacheManager = nodeCacheManager;
        this.phyNodeDao = phyNodeDao1;
    }


    protected AlignmentStatusType getTpRefAlignment(String tpId) {
        log.info("get tp ref alignment tpId:{}", tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Physical tpPhysical = terminationPointDao.getOpTpPhysical(
                neId, tpId);
        log.debug("tpPhysical: {}, alignmentStatus: {}",
                tpPhysical != null ? "found" : "null",
                tpPhysical != null ? tpPhysical.getAlignmentStatus() : "N/A");
        String transceiverId = PhysicalTpIdNamingRule.getTransceiverIdWithNamingRule(
                tpId);
        log.debug("transceiverId: {}", transceiverId);
        log.info("get equipments by eqId:{}", eqId);

        Node ne = phyNodeDao.getOpPhyNodeById(neId);
        if (ne == null) {
            Node configNe = phyNodeDao.getConfigPhyNodeById(neId);
            String vendorType = configNe.getAugmentation(Node1.class).getPhysical().getVendorType();
            if (vendorType.equals(VIRTUAL)) {
                log.debug("current ne is virtual ne,the neId:{}", neId);
                return AlignmentStatusType.Aligned;
            }
            log.warn("current ne id:{} is not managed,alignmentStatus unknown", neId);
            return AlignmentStatusType.Unknown;
        }
        List<Equipments> equipments = ne.getAugmentation(Node1.class).getPhysical().getEquipments();
        Optional<Equipments> refEquipment = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().equals(eqId
                )).findAny();
        log.debug("Total equipments found: {}", equipments.size());
        Optional<Equipments> transceiver = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(transceiverId)).findAny();
//        Equipments equipments = equipmentsDao.getOpEquipmentByNodeAndEqId(neId,
//                eqId);
//        Equipments transceiver = equipmentsDao.getOpEquipmentByNodeAndEqId(neId, transceiverId);
        log.debug("refEquipment present: {}, transceiver present: {}",
                refEquipment.isPresent(), transceiver.isPresent());
        List<AlignmentStatusType> alignmentStatusTypeList = new ArrayList<>();
        AlignmentStatusType tpAlign = AlignmentStatusType.Unknown;
        if (tpPhysical != null && tpPhysical.getAlignmentStatus() != null) {
            tpAlign = tpPhysical.getAlignmentStatus();

        }
        log.debug("tpAlign: {}", tpAlign);
        alignmentStatusTypeList.add(tpAlign);
        AlignmentStatusType eqAlign = AlignmentStatusType.Unknown;
        if (refEquipment.isPresent()) {
            eqAlign = refEquipment.get().getAlignmentStatus();
            log.debug("eqAlign from refEquipment: {}", eqAlign);
        }
        alignmentStatusTypeList.add(eqAlign);
        AlignmentStatusType tsAlign = null;
        if (transceiver.isPresent()) {
            log.debug("tsAlign: {}", tsAlign);
            tsAlign = transceiver.get().getAlignmentStatus();
            alignmentStatusTypeList.add(tsAlign);
        }
        log.info("Final alignmentStatusTypeList: {}", alignmentStatusTypeList);
        AlignmentStatusType result = StatusUtil.calculateAlignStatus(alignmentStatusTypeList);
        log.info("Calculated alignment status: {}", result);

        return result;
    }

    protected boolean isVirtualLink(String linkId) {
        String srcNeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
        String destNeId = PhysicalLinkIdNamingRule.getNodeZId(linkId);
        Map<String, Node> refNodeMap = nodeCacheManager.getConfigNodes(
                Arrays.asList(srcNeId, destNeId));
        Optional<Node> virtualNode = refNodeMap.values().stream()
                .filter(node -> node.getAugmentation(Node1.class).getPhysical().getVendorType()
                        .equals(VIRTUAL)).findAny();
        return virtualNode.isPresent();
    }


}
