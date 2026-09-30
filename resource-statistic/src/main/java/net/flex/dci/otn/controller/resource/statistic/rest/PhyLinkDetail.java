package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.resource.statistic.enums.LinkDirection;
import net.flex.dci.otn.controller.resource.statistic.enums.PortFlow;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class PhyLinkDetail implements Serializable {

    private String phyLinkId;

    private String phyLinkName;

    private String sourceSite;

    private String sourceNe;

    private String sourceTp;

    private String destinationSite;

    private String destinationNe;

    private String destinationTp;

    private String subnet;

    private String linkType;

    /**
     * 链路方向：BIDIRECTIONAL(双向), UNIDIRECTIONAL(单向)
     */
    private LinkDirection direction;

    /**
     * 源端口流向：OUT(出方向), IN(入方向), BIDIRECTIONAL(双向)
     */
    private PortFlow sourcePortFlow;

    /**
     * 目的端口流向：OUT(出方向), IN(入方向), BIDIRECTIONAL(双向)
     */
    private PortFlow destinationPortFlow;
}
