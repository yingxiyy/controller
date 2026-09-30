package net.flex.dci.otc.controller.notifier.core.domain.element;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/5/5 14:20
 */
@Data
@Builder
@AllArgsConstructor
public class ElementChange implements Serializable {

    @SerializedName("element-change")
    private ElementChangeBody elementChangeBody;
}
