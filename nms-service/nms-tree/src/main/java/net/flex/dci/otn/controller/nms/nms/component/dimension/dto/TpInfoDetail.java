package net.flex.dci.otn.controller.nms.nms.component.dimension.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TpInfoDetail implements Serializable {

    private String neId;

    private String siteId;

    private String cardId;

    private String tpId;
}
