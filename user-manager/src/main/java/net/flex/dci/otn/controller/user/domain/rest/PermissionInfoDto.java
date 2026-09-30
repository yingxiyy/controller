package net.flex.dci.otn.controller.user.domain.rest;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 9/12/2023 11:04 AM
 */
@Data
@Builder
public class PermissionInfoDto implements Serializable {
    private Long permissionId;
}
