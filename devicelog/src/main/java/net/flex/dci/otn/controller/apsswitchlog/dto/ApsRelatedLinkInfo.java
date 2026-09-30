package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 10/9/2025 4:54 PM
 */
@Data
@Builder
public class ApsRelatedLinkInfo implements Serializable {

    private List<ApsRelatedSiteLinkInfo> siteLinks;

    private List<ApsRelateTunnelInfo> tunnels;

}
