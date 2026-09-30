package net.flex.dci.otn.controller.nms.nms.component.terminationPoint;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 11/15/2023 10:52 AM
 */
@Slf4j
public abstract class AbstractNMSScanTerminationPoint implements INMSScanTerminationPoint {

    @Autowired
    protected NetconfTopology netconfTopology;

    protected List<String> getTerminationPointIdByIdRegexAndPortType(String regexId,
            PortType portType) {
        log.debug("");
        List<String> terminationPointIds = netconfTopology.getTerminationPointIdByIdRegexAndPortType(
                regexId, portType);
        return terminationPointIds;
    }
}
