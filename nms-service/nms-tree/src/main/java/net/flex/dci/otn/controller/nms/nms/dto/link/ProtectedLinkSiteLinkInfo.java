package net.flex.dci.otn.controller.nms.nms.dto.link;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/9/14
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ProtectedLinkSiteLinkInfo implements Serializable {

    private List<String> primarySiteLinkIds;

    private List<String> secondarySiteLinkIds;

    private List<String> tertiarySiteLinkIds;
}
