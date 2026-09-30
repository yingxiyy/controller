package net.flex.dci.otc.controller.otdr.components;

import net.flex.dci.otc.controller.otdr.model.link.PhyLinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter;

/**
 * @version 1.0
 * @date 9/7/2023 2:11 PM
 */
public interface PhyLinkAttenuation {

    void setBasePhyLinkAttenuationValue(PhyLinkInfo linkInfo, String monitorTpId, Double loss, StartOtdrParameter.MonitorDirection monitorDirection);
}
