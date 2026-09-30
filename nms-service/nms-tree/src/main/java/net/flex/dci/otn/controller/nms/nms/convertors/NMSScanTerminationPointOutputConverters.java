package net.flex.dci.otn.controller.nms.nms.convertors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2023 10:35 AM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NMSScanTerminationPointOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPoint, TerminationPoint> {

    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.TERMINATION_POINT;
    }


    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPoint> convert2NmsOutput(
            List<TerminationPoint> terminationPoints) {
        log.debug("start to convert termination point to nms output");
        if (terminationPoints.isEmpty()) {
            return new ArrayList<>();
        }
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPoint> terminationPointsOutput = terminationPoints.stream()
                .map(terminationPoint -> {
                    Physical terminationPointPhysical = terminationPoint.getAugmentation(
                            TerminationPoint1.class).getPhysical();
                    TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
                    terminationPointBuilder.setTpId(terminationPoint.getTpId())
                            .setKey(new TerminationPointKey(terminationPoint.getTpId()))
                            .setPhysical(terminationPointPhysical);
                    return terminationPointBuilder.build();
                }).collect(
                        Collectors.toList());
        return terminationPointsOutput;
    }
}
