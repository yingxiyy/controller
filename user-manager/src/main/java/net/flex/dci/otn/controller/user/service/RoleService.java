package net.flex.dci.otn.controller.user.service;

import java.util.List;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;

/**
 * @version 1.0
 * @date 2022/4/19 14:23
 */
public interface RoleService {

    List<RoleDto> listAllRoles();

    void createRole(RoleDto roleDto);

    void deleteRole(Long id);
}
