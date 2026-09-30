package net.flex.dci.otc.controller.scanner.port.helper.core.yang;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/5/2023 4:50 PM
 */
@Slf4j
@Component
public class TencentModelScannerPort implements YangModelScannerPort {


    @Override
    public NeYangModel supportYangModel() {
        return NeYangModel.Tencent;
    }

    /**
     * tecent model otdr and ocm integrated in one port
     *
     * @param equipType
     * @param targetTerminationPoint
     * @param equipmentRefTerminationPoints
     * @param monitorDirection
     * @return
     */
    @Override
    public TerminationPoint getOtdrPortRefBusinessPort(EquipType equipType,
            TerminationPoint targetTerminationPoint,
            List<TerminationPoint> equipmentRefTerminationPoints,
            MonitorDirection monitorDirection) {
        log.debug("get tencent model real otdr port");

        return targetTerminationPoint;
    }
}
