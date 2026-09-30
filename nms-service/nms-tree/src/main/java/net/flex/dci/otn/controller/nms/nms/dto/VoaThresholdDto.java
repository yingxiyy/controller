package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/7/6 11:05
 */
@Data
@Builder
public class VoaThresholdDto implements Serializable {


    private BigDecimal maxDestToSourceVoa;

    private BigDecimal maxSourceToDestVoa;

    private BigDecimal minDestToSourceVoa;

    private BigDecimal minSourceToDestVoa;


}
