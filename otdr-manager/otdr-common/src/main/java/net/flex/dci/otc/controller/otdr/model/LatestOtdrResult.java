package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/17/2023 10:52 AM
 */
@Data
@Builder
public class LatestOtdrResult implements Serializable {

    private OtdrCurrentDetail in;

    private OtdrCurrentDetail out;

}
