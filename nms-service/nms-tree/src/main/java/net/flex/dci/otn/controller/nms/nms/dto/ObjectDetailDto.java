package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 * 2025/12/28
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ObjectDetailDto implements Serializable {

    private String friendName;

    private AlarmSeverity alarmState;

    private AlignmentStatusType alignmentStatus;

    private ImplementState implementState;

    private OperStatus operStatus;

    private AdminStatus adminStatus;

    private String subnetId;

    private String subnetName;

    private Integer subnetLevel;
}
