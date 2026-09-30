package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/29 16:08
 */
@Data
@Builder
@AllArgsConstructor
public class SlotInfo implements Serializable {

    private String shelf;

    private String slot;

    private String port; //l/c

}
