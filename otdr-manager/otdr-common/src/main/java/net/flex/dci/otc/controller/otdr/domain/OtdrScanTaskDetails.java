package net.flex.dci.otc.controller.otdr.domain;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/11/27
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtdrScanTaskDetails implements Serializable {

    private Object input;

    private String errorMessage;
}
