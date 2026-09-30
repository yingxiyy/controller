package net.flex.dci.otc.controller.status.dto.operation;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 * @version 1.0
 * @date 2022/4/8 16:45
 */
@Data
@Builder
@AllArgsConstructor
public class OchLinksOperState implements Serializable {

    private List<OchLinkOperState> ochLinkOperStates;

    @Builder
    @Data
    @AllArgsConstructor
    public static class OchLinkOperState implements Serializable {

        private String linkId;

        private OperStatus operStatus;
    }
}
