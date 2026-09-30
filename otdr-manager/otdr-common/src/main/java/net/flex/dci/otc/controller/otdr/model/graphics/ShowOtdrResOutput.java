package net.flex.dci.otc.controller.otdr.model.graphics;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/18/2023 4:18 PM
 */
@Data
@Builder
public class ShowOtdrResOutput implements Serializable {

    private ShowOtdrResults output;

}
