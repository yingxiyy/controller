package net.flex.dci.otc.controller.status.dto.operation;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 * @version 1.0
 * @date 2022/4/8 16:44
 */
@Data
@Builder
@AllArgsConstructor
public class SiteLinksOperState implements Serializable {

    private List<SiteLinkOperState> siteLinkOperStates;

    @Data
    @Builder
    @AllArgsConstructor
    public static class SiteLinkOperState implements Serializable {

        private String siteLinkId;

        private OperStatus operStatus;
    }
}
