package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;

/**
 *
 * 2025/12/26
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class NeInfo implements Serializable {

    private String yangVersion;

    private String ip;

    private NodeType nodeType;

    private String vendor;
}
