package net.flex.dci.otn.controller.db.monitor.core.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/14 16:59
 */
@Data
@Builder
@AllArgsConstructor
public class Property implements Serializable {

    private String name;

    private String value;

}
