package net.flex.dci.otn.controller.nms.nms.dto.viewTopo;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;

/**
 * 2026/6/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewNodeTopoDto {

    private String viewNodeId;

    private SiteType siteType;

    private boolean hasNetworkElements;

    private ViewNodePhysical view;


    @Data
    @Builder
    public static class ViewNodePhysical implements Serializable {

        private String friendlyName;

        private Integer posX;

        private Integer posY;

        private String subnetId;

        private String subnetName;

        private Integer subnetLevel;

        private AlarmSeverity alarmState;
    }
}


