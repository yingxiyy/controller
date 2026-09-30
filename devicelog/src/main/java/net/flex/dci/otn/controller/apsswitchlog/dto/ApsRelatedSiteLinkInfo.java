package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 10/9/2025 4:56 PM
 */
@Data
@Builder
public class ApsRelatedSiteLinkInfo implements Serializable {

    private String siteLinkId;

    private String siteLinkName;

}
