package net.flex.dci.otn.controller.user.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ChangeType;
import net.flex.dci.otc.common.enums.ElementType;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import net.flex.dci.otn.controller.user.domain.PermissionDto;
import net.flex.dci.otn.controller.user.domain.notification.DeleteDto;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;
import net.flex.dci.otn.controller.user.utils.UserRoleConvertor;
import net.flex.dci.otn.db.jpa.entity.security.Permission;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.entity.security.User;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import net.flex.dci.otn.db.jpa.service.dao.UserDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/5 15:20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UaChangeMessage {

    private final RoleDaoService roleDaoService;

    private final UserDaoService userDaoService;

    public void notifyUserDelete(Long id) {
        log.debug("notify the user delete id is :{}", id);
        UserDto userDto = UserDto.builder().id(id).build();
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.DELETE)
                .elementType(ElementType.USER)
                .content(userDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyUserUpdate(User user, Role role) {
        log.debug("notify the user delete id is :{}", user.getId());
        String roleCode = role == null ? null : role.getRoleCode();
        String roleName = role == null ? null : role.getName();
        Long roleId = role == null ? null : role.getId();
        UserDto userDto = UserDto.builder().id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleId(roleId)
                .roleCode(roleCode)
                .roleName(roleName)
                .build();
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.UPDATE)
                .elementType(ElementType.USER)
                .content(userDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyUserCreate(User user, Long roleId) {
        log.debug("notify the user create id is :{}", user.getId());
        Role role = roleDaoService.findById(roleId);
        String roleCode = role == null ? null : role.getRoleCode();
        String roleName = role == null ? null : role.getName();

        UserDto userDto = UserDto.builder().id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createTime(user.getCreateTimestamp())
                .roleId(roleId)
                .roleCode(roleCode)
                .roleName(roleName)
                .build();
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.CREATE)
                .elementType(ElementType.USER)
                .content(userDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyUserUpdateRole(Long userId, Long roleId) {
        log.debug("notify the user update id is :{}", userId);
        Role role = roleDaoService.findById(roleId);
        UserDto userDto = UserDto.builder().id(userId)
                .roleId(roleId)
                .roleCode(role.getRoleCode())
                .roleName(role.getName())
                .build();
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.UPDATE)
                .elementType(ElementType.USER)
                .content(userDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyRoleCreate(Role role) {
        log.debug("notify the role create");
        RoleDto roleDto = UserRoleConvertor.convertRoleEntity2Dto(role);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.CREATE)
                .elementType(ElementType.ROLE)
                .content(roleDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyRoleDelete(Long id) {
        log.debug("notify the role create");
        DeleteDto roleDto = DeleteDto.builder().roleId(id).build();
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.DELETE)
                .elementType(ElementType.ROLE)
                .content(roleDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyPermissionCreate(Permission permission) {
        log.debug("notify the role create");
        PermissionDto permissionDto = UserRoleConvertor.convertPermissionEntity2Dto(permission);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.CREATE)
                .elementType(ElementType.PERMISSION)
                .content(permissionDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    public void notifyPermissionUpdate(Permission permission) {
        log.debug("notify the role create");
        PermissionDto permissionDto = UserRoleConvertor.convertPermissionEntity2Dto(permission);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(
                        ChangeType.UPDATE)
                .elementType(ElementType.PERMISSION)
                .content(permissionDto)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }
}
