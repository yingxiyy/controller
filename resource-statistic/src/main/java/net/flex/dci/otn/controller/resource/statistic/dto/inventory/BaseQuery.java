package net.flex.dci.otn.controller.resource.statistic.dto.inventory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseQuery implements Serializable {

    private int page = 1;
    private int limit = 20;
    private String siteId;
    private String neId;
    private String siteLinkId;
    private String tunnelId;
    private List<String> subnet;
    private Map<String, String> sort;
    private List<FilterCondition> filters;
}