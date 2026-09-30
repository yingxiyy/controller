package net.flex.dci.otc.controller.otdr.model.link;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class WrappedLink implements Serializable {

    private Boolean isAlign;

    private Link link;
}
