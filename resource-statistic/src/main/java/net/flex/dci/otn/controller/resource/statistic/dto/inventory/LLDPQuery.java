package net.flex.dci.otn.controller.resource.statistic.dto.inventory;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @version 1.0
 * @date 12/24/2025 11:00 AM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
public class LLDPQuery extends BaseQuery implements Serializable {
    
    private List<String> tunnelIds;
}
