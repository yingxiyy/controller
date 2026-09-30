package net.flex.dci.otn.controller.nms.operations.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 10/18/2023 1:42 PM
 */
@Data
public class TerminationPointDto implements Serializable {

    @JSONField(name = "termination-point")
    private List<Object> terminationPoint;

}
