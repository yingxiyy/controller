package net.flex.dci.otc.controller.otdr.model.breifly;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 9/4/2023 2:44 PM
 */
@Data
@Builder
public class OtdrBrieflyResultOutput implements Serializable {

    private OtdrBrieflyResultPaged output;
}
