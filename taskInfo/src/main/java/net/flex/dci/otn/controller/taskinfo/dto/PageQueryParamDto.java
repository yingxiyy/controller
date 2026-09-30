package net.flex.dci.otn.controller.taskinfo.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.taskinfo.enums.OrderElement;
import org.springframework.data.domain.Sort.Direction;

/**
 * @version 1.0
 * @date 2022/5/5 10:48
 */
@Data
@Builder
public class PageQueryParamDto implements Serializable {

    private Integer limit;

    private Integer page;

    private Direction direction;

    private OrderElement orderElement;

    private String keywords;

    private String userName;

    private String resourceType;

    private String actionType;

    private Boolean success;
}
