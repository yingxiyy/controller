package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/9/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SubNetInfo implements Serializable {

    private String name;

    private String subnetId;

}
