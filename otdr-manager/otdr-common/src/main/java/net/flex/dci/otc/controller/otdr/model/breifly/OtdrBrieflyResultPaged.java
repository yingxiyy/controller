package net.flex.dci.otc.controller.otdr.model.breifly;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @version 1.0
 * @date 9/4/2023 2:50 PM
 */
@Data
public class OtdrBrieflyResultPaged implements Serializable {

    private List<OtdrBrieflyResult> result;

    @JSONField(name = "total-records")
    private Integer totalRecords;
}
