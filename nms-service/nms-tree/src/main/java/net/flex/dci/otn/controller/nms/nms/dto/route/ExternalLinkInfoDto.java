package net.flex.dci.otn.controller.nms.nms.dto.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * 2026/1/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ExternalLinkInfoDto implements Serializable {

    private String linkId;

    private EquipType connectEquipType;

    private TerminationPoint sourceTp;

    private TerminationPoint destTp;

    private Equipments sourceEquip;

    private String sourceEquipId;

    private Equipments destEquip;

    private String destEquipId;

    private boolean isMpoGroup;

    private int mpoCount;

    private List<ExternalLinkInfoDto> mpoLinks;

}
