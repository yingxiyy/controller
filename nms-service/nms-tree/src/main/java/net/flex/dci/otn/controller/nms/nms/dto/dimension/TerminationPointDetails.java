package net.flex.dci.otn.controller.nms.nms.dto.dimension;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;

/**
 * 2026/4/7
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TerminationPointDetails implements Serializable {

    private String tpId;

    private String tpName;

    private PortType portType;

    private String cardId;

    private String neId;

}
