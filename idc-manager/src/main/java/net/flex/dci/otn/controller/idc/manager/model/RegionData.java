package net.flex.dci.otn.controller.idc.manager.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/27 10:24
 */
@Data
@Builder
@AllArgsConstructor
public class RegionData implements Serializable {

    private String name;

    private Long id;
}
