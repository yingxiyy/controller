package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.RouteEdgeType;

/**
 * @version 1.0
 * @date 2022/10/8 15:52
 */
@Data
public class RouteEdgeDto implements Serializable {

    private String srcTp;

    private String destTp;

    private String edgeId;

    private RouteEdgeType edgeType;


    private RouteEdgeType prev;

    private RouteEdgeType next;
}
