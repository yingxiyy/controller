package net.flex.dci.otn.controller.db.monitor.core.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/14 17:00
 */
@Data
@Builder
@AllArgsConstructor
public class Properties implements Serializable {

    private List<Property> property;
}
