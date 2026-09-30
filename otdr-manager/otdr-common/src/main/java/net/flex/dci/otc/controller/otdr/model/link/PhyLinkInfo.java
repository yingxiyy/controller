package net.flex.dci.otc.controller.otdr.model.link;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 9/7/2023 1:42 PM
 */
@Data
@Builder
public class PhyLinkInfo implements Serializable {

    private String linkId;

    private String srcTpId;

    private String destTpId;
}
