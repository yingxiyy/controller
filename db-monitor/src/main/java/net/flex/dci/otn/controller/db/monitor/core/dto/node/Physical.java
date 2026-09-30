package net.flex.dci.otn.controller.db.monitor.core.dto.node;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.db.monitor.core.dto.Properties;

/**
 * @version 1.0
 * @date 2022/11/14 16:58
 */
@Data
@Builder
@AllArgsConstructor
public class Physical implements Serializable {

    private Properties properties;

}
