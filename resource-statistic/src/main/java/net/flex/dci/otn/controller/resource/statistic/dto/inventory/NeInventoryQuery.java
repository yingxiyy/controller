package net.flex.dci.otn.controller.resource.statistic.dto.inventory;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/10/25
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class NeInventoryQuery implements Serializable {

    private int page;
    private int limit;

}
