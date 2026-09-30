package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/1/26 15:03
 */
@Data
@Builder
public class PageIdcData implements Serializable {

    @JSONField(name = "idc")
    private List<IdcData> idcData;

    @JSONField(name = "current-page")
    private Long currentPage;

    @JSONField(name = "total-elements")
    private Long totalElements;

    @JSONField(name = "total-pages")
    private Integer totalPages;
}
