package net.flex.dci.otc.controller.status.dto.align;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 * @version 1.0
 * @date 2022/4/7 15:35
 */
@Data
@Builder
@AllArgsConstructor
public class PhyLinksAlignState implements Serializable {

    private List<PhyLinkAlignState> phyLinkAlignStates;

    @Data
    @Builder
    @AllArgsConstructor
    public static class PhyLinkAlignState implements Serializable {

        private String linkId;

        private AlignmentStatusType alignmentStatusType;
    }
}
