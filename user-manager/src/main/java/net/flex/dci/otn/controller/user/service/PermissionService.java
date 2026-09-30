package net.flex.dci.otn.controller.user.service;

import java.util.List;
import net.flex.dci.otn.controller.user.domain.PermissionDto;
import net.flex.dci.otn.controller.user.domain.RolePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.AssignPermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateOrUpdatePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.RemovePermissionDto;

/**
 * @version 1.0
 * @date 2022/4/22 16:34
 */
public interface PermissionService {

    Long createPermission(CreateOrUpdatePermissionDto createOrUpdatePermissionDto);

    void updatePermission(CreateOrUpdatePermissionDto createOrUpdatePermissionDto);

    List<PermissionDto> listAllPermission();

    RolePermissionDto listAllPermissionByRole(Long id);

    void assignPermission(AssignPermissionDto assignPermissionDto);

    void addRolePermission(AssignPermissionDto assignPermissionDto);

    void removePermission(RemovePermissionDto removePermissionDto);
}
