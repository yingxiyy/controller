package net.flex.dci.otn.controller.nms.nms.dto.site.route;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp;

/**
 * @version 1.0
 * @date 2022/12/21 17:42
 */
@Data
public class SiteRefPhyNodeRouteDto implements Serializable {

    private List<ResourceType> ILATps = new ArrayList<>();

    private List<ResourceType> OATps = new ArrayList<>();

    private List<ResourceType> WSSTps = new ArrayList<>();

    private List<ResourceType> MUXTps = new ArrayList<>();

    private List<ResourceType> muxPanelTpS = new ArrayList<>();

    private List<ResourceType> phyLinks = new ArrayList<>();


    private static final String SIG = "SIG";

    /**
     * sort the oa tps
     *
     * @return
     */
    public List<ResourceType> getOATps() {
        if (!OATps.isEmpty()) {
            ResourceType tpResourceType = OATps.get(0);
            String tpId = ((Tp) tpResourceType).getTpHop().getPhyTp().getTpId().getValue();
            if (tpId.endsWith(SIG)) {
                Collections.reverse(OATps);
            }
        }
        return OATps;
    }


}
