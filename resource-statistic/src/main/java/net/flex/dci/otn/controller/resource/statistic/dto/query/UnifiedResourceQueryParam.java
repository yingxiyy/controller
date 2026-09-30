package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;

/**
 * 2026/7/12
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnifiedResourceQueryParam implements Serializable {

    private InventoryType unifiedType;

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

    private String neName;

    private String vendorName;

    /**
     * Tunnel ID列表
     */
    private List<String> tunnelIds;

//    private List<FilterCondition> filters;

//    private List<String> outputFields;

    private Boolean includeSummary = false;
}
