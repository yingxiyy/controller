package net.flex.dci.otn.controller.resource.statistic.export;

import com.alibaba.excel.annotation.ExcelIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SiteLink 业务网元导出 CSV 数据模型 按照业务顺序：A端站点 -> A端网元 -> Z端站点 -> Z端网元 -> 链路信息
 *
 * @author musa
 * @version 1.0
 * @date 2026/4/4
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteLinkCsv implements Serializable {

    private String siteLinkId;


    private String siteLinkName;

    @ExcelIgnore
    private String sourceSiteId;


    private String sourceSiteName;

    /**
     * A端网元ID
     */
    @ExcelIgnore
    private String sourceNeId;

    /**
     * A端网元名称
     */
    private String sourceNeName;


    /**
     * A端端口ID
     */
    @ExcelIgnore
    private String sourceTpId;

    /**
     * A端端口名称
     */
    private String sourceTpName;

    /**
     * Z端站点ID
     */
    @ExcelIgnore
    private String destinationSiteId;

    /**
     * Z端站点名称
     */
    private String destinationSiteName;

    /**
     * Z端网元ID
     */
    @ExcelIgnore
    private String destinationNeId;

    /**
     * Z端网元名称
     */
    private String destinationNeName;


    /**
     * Z端端口ID
     */
    @ExcelIgnore
    private String destinationTpId;

    /**
     * Z端端口名称
     */
    private String destinationTpName;

    /**
     * 所属网络/平面
     */
    private String subnet;


    private String implementState;


    private String msModel;
    private String protectionType;

    private String bandwidth;

    private String demandSource;

    private String creationTime;

    private String activationTime;

}
