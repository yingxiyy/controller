package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/9/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SiteLinkInfo implements Serializable {

    private String siteLinkId;

    private String siteLinkName;

}
