package net.flex.dci.otc.controller.notifier.core.domain.element;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/5/5 14:21
 */
@Data
@Builder
@AllArgsConstructor
public class ElementChangeBody implements Serializable {

    @SerializedName("element-type")
    private String elementType;

    @SerializedName("change-type")
    private String changeType;

    @SerializedName("change-body")
    private Object content;

}
