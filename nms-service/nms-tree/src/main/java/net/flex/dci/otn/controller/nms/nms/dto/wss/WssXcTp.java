package net.flex.dci.otn.controller.nms.nms.dto.wss;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/29 15:36
 */
@Data
@Builder
public class WssXcTp implements Serializable {

    private String notSigPort;

    private String sigPort;
}
