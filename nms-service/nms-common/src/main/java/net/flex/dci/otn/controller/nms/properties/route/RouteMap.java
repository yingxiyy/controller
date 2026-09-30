package net.flex.dci.otn.controller.nms.properties.route;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/2/15 10:23
 */
@Data
public class RouteMap implements Serializable {

    @JSONField(name = "desc")
    private String description;

    @JSONField(name = "instance")
    private List<Instance> instances;

}
