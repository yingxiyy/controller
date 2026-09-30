package net.flex.dci.otn.controller.user.permission.authorization.impl;

import static net.flex.dci.otc.common.constants.AuthConstant.RESOURCE_ROLES_ANT_MAP_KEY;
import static net.flex.dci.otc.common.constants.AuthConstant.RESOURCE_ROLES_DIRECT_MAP_KEY;

import com.google.gson.reflect.TypeToken;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otn.controller.user.permission.authorization.PermissionRoleLoadHandler;
import net.flex.dci.otn.db.jpa.entity.security.Permission;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.service.dao.PermissionDaoService;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/25 14:06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionRoleLoadHandlerImpl implements PermissionRoleLoadHandler {

    private final RoleDaoService roleDaoService;

    private final PermissionDaoService permissionDaoService;

    private final RedisCacheOperation cacheOperation;


    @Override
    public void loadThePermissionRole() {
        log.debug("start to load the permission role relation map to the cache");
        if (RedisLockUtil.tryLock(AuthConstant.ROLE_PERMISSION_MAP_LOCK, TimeUnit.SECONDS, 2, 5)) {
            List<Role> roles = roleDaoService.findAll();
            cacheOperation.deleteKey(RESOURCE_ROLES_ANT_MAP_KEY);
            cacheOperation.deleteKey(RESOURCE_ROLES_DIRECT_MAP_KEY);
            loadThePermissionRole(roles);
        }
    }

    public void loadThePermissionRole(List<Role> roles) {
        roles.forEach(role -> {
            List<Permission> permissions = permissionDaoService.listAllPermissionByRoleId(
                    role.getId());
            if (permissions == null || permissions.isEmpty()) {
                return;
            }
            permissions.forEach(permission -> {
                load2PermissionCache2Map(permission, role);
            });
        });
    }

    @Override
    public void load2PermissionCache2Map(Permission permission, Role role) {
        log.debug("load the role permission to the cache");
        String path = permission.getPath();
        String url = permission.getUrl();
        String roleCode = role.getRoleCode();
        String type = permission.getType() == null ? AuthConstant.BLANK : permission.getType();

        if (url == null) {
            Set<String> authorities = cacheOperation.getValue(
                    RESOURCE_ROLES_ANT_MAP_KEY, path, new TypeToken<HashSet<String>>() {
                    }.getType());
            if (authorities == null) {
                authorities = new HashSet<>();
            }
            authorities.add(roleCode);
            cacheOperation.set(RESOURCE_ROLES_ANT_MAP_KEY, path, authorities);
        } else {
            String subKey = url + AuthConstant.AT + type.toUpperCase();
            Set<String> authorities = cacheOperation.getValue(
                    RESOURCE_ROLES_DIRECT_MAP_KEY, subKey, new TypeToken<HashSet<String>>() {
                    }.getType());
            if (authorities == null) {
                authorities = new HashSet<>();
            }
            authorities.add(roleCode);
            cacheOperation.set(RESOURCE_ROLES_DIRECT_MAP_KEY, subKey, authorities);
        }
    }
}
