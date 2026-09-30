package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class PhyNodeInfo implements Serializable {

    private String nodeId;

    private String name;

    private String ip;
}
