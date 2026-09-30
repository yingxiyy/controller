package net.flex.dci.otn.controller.db.monitor.core.dto.adapter;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.db.monitor.core.dto.RegisteredNe;

/**
 * @version 1.0
 * @date 2022/11/16 13:36
 */
@Data
@Builder
@AllArgsConstructor
public class AdapterData implements Serializable {

    @JSONField(name = "adapter")
    private List<RegisteredNe> adapterNe;
}
