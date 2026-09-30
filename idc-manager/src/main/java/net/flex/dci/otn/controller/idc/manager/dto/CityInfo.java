package net.flex.dci.otn.controller.idc.manager.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/10/26 15:57
 */
@Data
@Builder
public class CityInfo implements Serializable {

    private String cityName;

    private Long cityId;

}
