package net.flex.dci.otc.controller.status.dto;

import java.io.Serializable;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.status.enums.ProtectionActivePathRole;

/**
 * @version 1.0
 * @date 8/22/2023 4:09 PM
 */
@Data
@Builder
public class ProtectionMode implements Serializable {

    private Set<ProtectionActivePathRole> activePathRoles;
}
