package net.flex.dci.otn.controller.nms.nms.dto.viewTopo;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * 2026/6/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewLinkTopoDto implements Serializable {

    private String viewLinkId;

    private List<String> supportLinkIds;

    private String sourceNode;

    private String destNode;

    private ViewLinkPhysical viewLinkPhysical;

    @Data
    @Builder
    public static class ViewLinkPhysical {

        private Integer bundleNum;
        private AlarmSeverity alarmState;

        private String subnetId;

        private String subnetName;

        private ViewLinkType linkType;

        private Integer subnetLevel;
    }
}
