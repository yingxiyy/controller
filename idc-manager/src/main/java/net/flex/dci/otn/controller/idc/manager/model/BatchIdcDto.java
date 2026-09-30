package net.flex.dci.otn.controller.idc.manager.model;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class BatchIdcDto implements Serializable {

    private List<Long> id;

    private Boolean enable;
}
