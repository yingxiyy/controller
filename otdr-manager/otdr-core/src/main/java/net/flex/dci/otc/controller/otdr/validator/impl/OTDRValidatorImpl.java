package net.flex.dci.otc.controller.otdr.validator.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.otdr.validator.OTDRValidator;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/2/2023 2:14 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OTDRValidatorImpl implements OTDRValidator {

    private final PhyLinkDao phyLinkDao;

    @Override
    public void validateMonitorSituation(String monitorPortId, Node node) {
        log.debug("validate the monitor situation,the monitor port id is:{} node id is:{}",
                monitorPortId, node.getNodeId().getValue());
        Map<String, TerminationPoint> terminationPointIdMaps = node.getTerminationPoint().stream()
                .collect(HashMap::new, (map, tp) -> map.put(tp.getTpId().getValue(), tp),
                        HashMap::putAll);
        String friendlyName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        if (!terminationPointIdMaps.containsKey(monitorPortId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne %s monitor port:%s is invalided", friendlyName,
                            monitorPortId));
        }
        NeYangModel neYangModel = NeYangModel.getModel(
                node.getAugmentation(Node1.class).getPhysical());
        if (neYangModel.equals(NeYangModel.ChinaTelecom)) {
            //todo: china telecom model equipment,should have the OTDR link for the ne
            TerminationPoint monitorTp = terminationPointIdMaps.get(monitorPortId);
            Physical tpPhysical = monitorTp.getAugmentation(
                    TerminationPoint1.class).getPhysical();
            PortType tpType = tpPhysical.getPortType();
            if (!tpType.equals(PortType.OTDR)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("the ne %s monitor port:%s is not OTDR type port",
                                friendlyName,
                                monitorPortId));
            }
            List<Link> otdrLinks = phyLinkDao.listAllPhyLinksUnderTp(
                            monitorPortId).stream()
                    .filter(link -> link.getAugmentation(Link1.class).getPhysical().getLinkType()
                            .equals(
                                    LinkType.OtdrLink)).collect(Collectors.toList());
            if (otdrLinks.isEmpty()) {
                throw new CommonException(CommonExceptionType.NO_OTDR_LINK_ERROR,
                        String.format(
                                "The ne %s monitor port:%s should be established a fiber connection before proceeding with OTDR",
                                friendlyName, monitorPortId));
            }
        }
    }
}
