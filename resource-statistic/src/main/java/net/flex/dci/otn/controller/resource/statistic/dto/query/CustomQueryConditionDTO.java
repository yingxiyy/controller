package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomQueryConditionDTO implements Serializable {

    private MaterialQueryDTO materialQuery;

    private CircuitQueryDTO tunnelQuery;

    private PerformanceQueryDTO performanceQuery;
}
