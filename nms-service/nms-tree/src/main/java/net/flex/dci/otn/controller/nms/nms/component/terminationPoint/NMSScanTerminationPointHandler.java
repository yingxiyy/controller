package net.flex.dci.otn.controller.nms.nms.component.terminationPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/15/2023 10:51 AM
 */
@Component
@Slf4j
public class NMSScanTerminationPointHandler {

    private final Map<RetrieveType, INMSScanTerminationPoint> terminationPointMap;

    public NMSScanTerminationPointHandler(List<INMSScanTerminationPoint> nmsTerminationPointList) {
        terminationPointMap = nmsTerminationPointList.stream().collect(HashMap::new,
                (map, terminationPoint) -> map.put(terminationPoint.supportType(),
                        terminationPoint), HashMap::putAll);
    }

    public List<TerminationPoint> getElementRefScanTerminationPoints(
            String siteRef, String tpRef, PortType portType, OtdrPortDirection otdrPortDirection) {
        log.debug(
                "get element ref termination points base on input:{},tpRef:{},portType:{},otdrPortDirection:{}",
                siteRef, tpRef, portType, otdrPortDirection);
        if (Objects.isNull(portType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the port type should not be null");
        }
        if (Objects.isNull(siteRef) && Objects.isNull(tpRef)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the site ref or tp ref can not be null");
        }
        if (!Objects.isNull(otdrPortDirection) && portType.equals(PortType.MON)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the port only support ocm scan,direction should be null");
        }
        if (Objects.isNull(otdrPortDirection) && portType.equals(PortType.OTDR)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the port is otdr port, otdr direction should not be null");
        }
        RetrieveType retrieveType = RetrieveType.DEFAULT;
        String refElementId = null;
        if (siteRef != null && tpRef == null) {
            retrieveType = RetrieveType.SITE_NODE;
            refElementId = siteRef;
        } else if (tpRef != null && siteRef == null) {
            retrieveType = RetrieveType.EQUIPMENT;
            refElementId = tpRef;
        }

        return terminationPointMap.get(retrieveType)
                .getElementRefUnOccupiedTerminationPointsByElementId(refElementId, portType,
                        otdrPortDirection);
    }

}
