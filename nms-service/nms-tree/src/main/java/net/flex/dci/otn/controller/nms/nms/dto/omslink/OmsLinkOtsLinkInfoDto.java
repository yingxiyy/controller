package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/8/9
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OmsLinkOtsLinkInfoDto implements Serializable {

    private String omsLinkId;

    private String omsName;

    private RouteContractInfoDto routeContractInfoDto;

    private List<OtsLinkAmplifierInfo> otsLinks;

    private List<PhyNodeInfo> phyNodeInfos;

    private OmsLinkPAInfo paInfo;
}
