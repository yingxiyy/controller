package net.flex.dci.otc.controller.status.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.status.enums.ProtectionActivePathRole;

/**
 * @version 1.0
 * @date 2022/8/30 23:51
 */
@Data
@Builder
public class ProtectedLinkDto implements Serializable {

//    private boolean isPrimary;
//
//    private boolean isSecondary;
//
//    private boolean isThird;

    private Set<ProtectionActivePathRole> activePathRoles;

    private List<String> primaryLinkIds;

    private List<String> secondaryLinkIds;

    private List<String> tertiaryLinkIds;
}
