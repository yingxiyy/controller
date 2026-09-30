package net.flex.dci.otn.controller.allocate.designer.bom;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"物料型号", "设备类型", "物料名称", "物料类别", "物料卡类", "物料类别配置","PN号","FRU"})
public class BomMetaInfo {

    @NonNull
    @JsonProperty("物料型号")
    private String serialNo;//物料型号

    @NonNull
    @JsonProperty("设备类型")
    private String equipType;//设备类型

    @NonNull
    @JsonProperty("物料名称")
    private String name;//物料名称

    @NonNull
    @JsonProperty("物料类别")
    private String componentType;//物料类别，比如CHASSIS，LINECARD

    @NonNull
    @JsonProperty("物料卡类")
    private String cardClass;

    @NonNull
    @JsonProperty("物料类别配置")
    private String equipTypeConfiged;

    @JsonProperty("PN号")
    private String pn;

    @JsonProperty("FRU")
    private String fru;
}
