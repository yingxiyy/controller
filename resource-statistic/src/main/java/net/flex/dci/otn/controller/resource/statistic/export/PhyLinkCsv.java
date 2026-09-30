package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class PhyLinkCsv implements Serializable {

    @ColumnWidth(1)
    private String phyLinkId;

    private String phyLinkName;

    private String sourceSite;

    private String sourceNe;

    private String sourceTp;

    private String destinationSite;

    private String destinationNe;

    private String destinationTp;

    private String subnet;

    /**
     * 链路方向：BIDIRECTIONAL(双向), UNIDIRECTIONAL(单向)
     */
    private String direction;

//    /**
//     * 源端口流向：OUT(出方向), IN(入方向), BIDIRECTIONAL(双向)
//     */
//    private String sourcePortFlow;
//
//    /**
//     * 目的端口流向：OUT(出方向), IN(入方向), BIDIRECTIONAL(双向)
//     */
//    private String destinationPortFlow;
}
