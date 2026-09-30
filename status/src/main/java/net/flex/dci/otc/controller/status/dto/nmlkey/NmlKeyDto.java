package net.flex.dci.otc.controller.status.dto.nmlkey;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/9 17:50
 */
@Data
@Builder
@AllArgsConstructor
public class NmlKeyDto implements Serializable {

    private String siteNodeId;

    private String phyNodeId;

    private String equipId;

    private String tpId;

    private String transceiverId;
}
