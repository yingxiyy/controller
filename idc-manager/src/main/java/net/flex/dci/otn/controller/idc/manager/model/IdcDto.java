package net.flex.dci.otn.controller.idc.manager.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/1/28 16:59
 */
@Data
@Builder
public class IdcDto implements Serializable {

    @Tolerate
    public IdcDto() {

    }

    private Long id;

    private Boolean enable;

}
