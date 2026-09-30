package net.flex.dci.otn.controller.nms.properties.route;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/2/15 13:18
 */
@Data
public class Instance implements Serializable {

    private String name;

    private List<String> prefix;

    @JSONField(name = "sub-path")
    private List<String> subPaths;
}
