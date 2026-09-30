package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OsLinkInfo implements Serializable {

    private String osLinkId;

    private String sourceNeId;

    private String sourceTpId;

    private String sourceTpName;

    private String destNeId;

    private String destTpId;

    private String destTpName;

}
