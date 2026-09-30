package net.flex.dci.otn.controller.module;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 9/29/2025 4:24 PM
 */
@Data
@Builder
public class ApsRequest implements Serializable {

    private String neId;

    private String apsName;
}
