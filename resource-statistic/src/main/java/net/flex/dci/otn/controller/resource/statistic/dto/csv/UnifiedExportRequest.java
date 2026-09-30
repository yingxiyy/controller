package net.flex.dci.otn.controller.resource.statistic.dto.csv;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ExportFormat;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryScope;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;

/**
 * 统一导出请求 DTO 支持 CSV 和 Excel 两种导出格式
 *
 * @author musa
 * @version 1.0
 * @date 2025/11/2
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnifiedExportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 导出类型：NE/TUNNEL/LLDP/CARD/TRANSCEIVER
     */
    private InventoryType unifiedType;

    /**
     * 导出范围：NE/SITE/SITE_LINK/TUNNEL/PLANE/ALL
     */
    private InventoryScope scope;

    /**
     * 导出格式：CSV/EXCEL，默认 CSV
     */
    private ExportFormat format = ExportFormat.EXCEL;

    /**
     * 子网/平面ID列表
     */
    private List<String> subnet;

    /**
     * 站点ID列表
     */
    private List<String> siteIds;

    /**
     * 网元ID列表（直接指定）
     */
    private List<String> neIds;

    /**
     * phy link ids
     */
    private List<String> phyLinkIds;

    /**
     * SiteLink ID列表
     */
    private List<String> siteLinkIds;

    /**
     * Tunnel ID列表
     */
    private List<String> tunnelIds;


    private List<FilterCondition> filters;


}
