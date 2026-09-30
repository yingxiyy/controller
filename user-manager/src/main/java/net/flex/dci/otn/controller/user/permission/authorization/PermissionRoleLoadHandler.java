package net.flex.dci.otn.controller.user.permission.authorization;

import net.flex.dci.otn.db.jpa.entity.security.Permission;
import net.flex.dci.otn.db.jpa.entity.security.Role;

/**
 * @version 1.0
 * @date 2022/4/25 14:06
 */
public interface PermissionRoleLoadHandler {

    void loadThePermissionRole();

    void load2PermissionCache2Map(Permission permission, Role role);
}
