package net.flex.dci.otn.controller.user.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.user.enums.OrderElement;
import org.springframework.data.domain.Sort.Direction;

/**
 * @version 1.0
 * @date 2022/4/19 15:14
 */
@Data
@Builder
@AllArgsConstructor
public class PageQueryParamDto implements Serializable {

    private Integer page;

    private Integer limit;

    private String keywords;

    private Direction direction = Direction.DESC;

    private OrderElement orderElement;

}
