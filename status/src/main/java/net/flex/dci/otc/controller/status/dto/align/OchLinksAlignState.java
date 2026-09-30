package net.flex.dci.otc.controller.status.dto.align;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 * @version 1.0
 * @date 2022/4/8 12:31
 */
@Data
@Builder
@AllArgsConstructor
public class OchLinksAlignState implements Serializable {

    List<OchLinkAlignState> alignStates;

    @Builder
    @Data
    @AllArgsConstructor
    public static class OchLinkAlignState {

        private String ochLinkId;

        private AlignmentStatusType alignmentStatusType;
    }
}
