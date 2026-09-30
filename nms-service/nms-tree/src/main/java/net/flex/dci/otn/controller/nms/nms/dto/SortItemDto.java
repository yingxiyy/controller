package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/5/27 10:41
 */
@Data
@Builder
public class SortItemDto implements Serializable {

    private boolean ascending;

    private String sortName;
}
