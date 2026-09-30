package net.flex.dci.otn.controller.system.config.cache.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/3/13 23:00
 */
@Data
@Builder
public class PhyNodeRefName implements Serializable {

    @Tolerate
    public PhyNodeRefName() {

    }

    private String id;

    private String friendlyName;

}
