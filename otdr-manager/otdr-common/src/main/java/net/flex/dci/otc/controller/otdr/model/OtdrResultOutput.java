package net.flex.dci.otc.controller.otdr.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/8/3
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtdrResultOutput implements Serializable {

    private String tpId;

    private String content;

    private Object scanParameters;

    private String scanMode;
}
