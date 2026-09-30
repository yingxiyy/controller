package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/17/2023 11:19 AM
 */
@Data
@Builder
public class LatestOtdrResOutput implements Serializable {

    private LatestOtdrResult output;

}
