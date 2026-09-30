package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.common.constants.Constants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;

/**
 * @version 1.0
 * @date 2022/3/20 11:32
 */
@Data
@Builder
@AllArgsConstructor
public class TerminationPointCache extends BaseCache implements Serializable {

    private String id;

    private String friendlyName;

    public static TerminationPointCache getFromTerminationPoint(String tpId, TerminationPoint tp) {
        String friendlyName = null;
        if (tp == null) {
            String ids[] = tpId.split(Constants.POUND);
            friendlyName = ids[ids.length - 1];
        } else {
            TerminationPoint1 physicalProperty = tp.getAugmentation(TerminationPoint1.class);
            friendlyName = physicalProperty.getPhysical().getFriendlyName();
        }
        return TerminationPointCache.builder().id(tpId)
                .friendlyName(friendlyName).build();
    }
}
