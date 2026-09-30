package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.flex.dci.otn.controller.resource.statistic.enums.PerformanceLevel;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class PerformanceQueryDTO extends BaseQueryDTO {

    public PerformanceLevel performanceLevel;

    private List<String> performanceGroupList;

    private Long collectTime;

}
