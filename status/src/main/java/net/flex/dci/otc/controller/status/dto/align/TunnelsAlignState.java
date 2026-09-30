package net.flex.dci.otc.controller.status.dto.align;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 * @version 1.0
 * @date 2022/4/8 12:33
 */
@Builder
@Data
@AllArgsConstructor
public class TunnelsAlignState implements Serializable {

    List<TunnelAlignState> tunnelAlignStates;

    @Builder
    @Data
    @AllArgsConstructor
    public static class TunnelAlignState {

        private String tunnelId;

        private AlignmentStatusType alignmentStatusType;
    }
}
