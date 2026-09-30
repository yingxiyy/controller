package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.terminationPoint.AbstractNMSScanTerminationPoint;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import net.flex.dci.otn.controller.nms.properties.scan.TelecomScanPortConfiguration;
import net.flex.dci.otn.controller.nms.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/15/2023 10:53 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteRefNMSScanTerminationPoint extends AbstractNMSScanTerminationPoint {


    private final TelecomScanPortConfiguration telecomScanPortConfiguration;


    @Override
    public RetrieveType supportType() {
        return RetrieveType.SITE_NODE;
    }

    @Override
    public List<TerminationPoint> getElementRefUnOccupiedTerminationPointsByElementId(
            String refElementId, PortType portType, OtdrPortDirection otdrPortDirection) {
        log.debug("get unOccupied termination point by site id :{} the portType is:{}",
                refElementId, portType);
        EquipType[] equipTypes = telecomScanPortConfiguration.getSupportScanCardType();
        List<String> refEquipmentIds = netconfTopology.listAllOperationalEquipmentByIdRegexAndEquipTypes(
                refElementId,
                equipTypes);
        List<String> refTerminationPointIds = netconfTopology.getTerminationPointIdByRefEquipIdsAndPortType(
                refEquipmentIds, portType);
        List<String> notOccupiedTerminationPointIds = filterUnOccupiedTerminationPointIds(
                refTerminationPointIds);
        List<TerminationPoint> refTerminationPoints = netconfTopology.getAllTerminationPointByIds(
                notOccupiedTerminationPointIds);
        return refTerminationPoints;
    }

    /**
     * filter un occupied termination point ids,means not in physical link,the link type is otdr and
     * ocm
     *
     * @param refTerminationPointIds
     * @return
     */
    private List<String> filterUnOccupiedTerminationPointIds(
            List<String> refTerminationPointIds) {
        log.debug("filter the unOccupied termination point ids in :{}", refTerminationPointIds);
        Set<String> refEquipmentIds = refTerminationPointIds.stream()
                .map(PhysicalTpIdNamingRule::getEquipId).collect(
                        Collectors.toSet());
        List<Link> refScanLink = netconfTopology.listAllPhyLinkUnderEquipments(refEquipmentIds);
        List<String> occupiedTpIds = refScanLink.stream()
                .map(link -> Arrays.asList(link.getSource().getSourceTp().getValue(),
                        link.getDestination().getDestTp().getValue())).flatMap(
                        Collection::stream).collect(Collectors.toList());
        Set<String> unOccupiedTpIds = CommonUtils.getDifferenceSetByGuava(
                new HashSet<>(refTerminationPointIds), new HashSet<>(occupiedTpIds));
        return new ArrayList<>(unOccupiedTpIds);
    }

}
