package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.flex.dci.otn.controller.apsswitchlog.enums.OrderElement;

/**
 *
 * 2025/9/13
 *
 * @author musa
 * @version 1.0
 **/
@Data
@AllArgsConstructor
public class SortCondition implements Serializable {

    private OrderElement orderElement;

    private String direction;

}
