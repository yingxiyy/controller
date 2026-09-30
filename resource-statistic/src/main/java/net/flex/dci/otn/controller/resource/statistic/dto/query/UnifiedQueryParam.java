package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;

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
public class UnifiedQueryParam implements Serializable {

    private List<String> subnet;

    private CustomQueryConditionDTO queryCondition;

    private ResourceQueryType resourceQueryType;
}
