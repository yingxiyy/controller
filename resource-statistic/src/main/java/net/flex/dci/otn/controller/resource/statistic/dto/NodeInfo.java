package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 11/3/2025 5:16 PM
 */
@Data
@Builder
@AllArgsConstructor
public class NodeInfo implements Serializable {

    private String neId;

    private String neName;

}
