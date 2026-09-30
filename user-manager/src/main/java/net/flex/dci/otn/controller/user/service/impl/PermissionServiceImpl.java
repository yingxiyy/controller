package net.flex.dci.otn.controller.user.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.user.domain.PermissionDto;
import net.flex.dci.otn.controller.user.domain.RolePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.AssignPermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateOrUpdatePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.RemovePermissionDto;
import net.flex.dci.otn.controller.user.message.UaChangeMessage;
import net.flex.dci.otn.controller.user.permission.authorization.PermissionRoleLoadHandler;
import net.flex.dci.otn.controller.user.service.PermissionService;
import net.flex.dci.otn.controller.user.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.user.utils.UserRoleConvertor;
import net.flex.dci.otn.db.jpa.entity.security.Permission;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.service.dao.PermissionDaoService;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/4/22 16:35
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionDaoService permissionDaoService;

    private final RoleDaoService roleDaoService;

    private final PermissionRoleLoadHandler permissionRoleLoadHandler;

    private final UaChangeMessage uaChangeMessage;

    @Override
    public Long createPermission(CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        log.debug("create a new permission");
        Permission permission = UserRoleConvertor.convertCreatePermissionDtoToEntity(
                createOrUpdatePermissionDto);
        Permission dbPermission = permissionDaoService.save(permission);
        uaChangeMessage.notifyPermissionCreate(dbPermission);
        return dbPermission.getId();
    }

    @Override
    public void updatePermission(CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        log.debug("create a new permission");
        Permission permission = UserRoleConvertor.convertCreatePermissionDtoToEntity(
                createOrUpdatePermissionDto);
        Permission dbPermission = permissionDaoService.save(permission);
        uaChangeMessage.notifyPermissionCreate(dbPermission);
        AsynchronousExecutor.execute(permissionRoleLoadHandler::loadThePermissionRole);
    }

    @Override
    public List<PermissionDto> listAllPermission() {
        log.info("list all permission");
        List<Permission> permissions = permissionDaoService.findAll();
        List<PermissionDto> permissionDtos = UserRoleConvertor.convertPermissionEntities2Dtos(
                permissions);
        return permissionDtos;
    }

    @Override
    public RolePermissionDto listAllPermissionByRole(Long id) {
        log.debug("start to get all permission about the role :{}", id);
        //todo: validated the role id
        Role role = roleDaoService.findById(id);
        if (role == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role id: %d is not found", id));
        }
        List<Permission> permissions = permissionDaoService.listAllPermissionByRoleId(id);
        RolePermissionDto rolePermissionDto = UserRoleConvertor.convertRolePermissions2Dto(role,
                permissions);
        return rolePermissionDto;
    }

    @Override
    public void assignPermission(AssignPermissionDto assignPermissionDto) {
        log.debug("assign the permission to the role ,{}", assignPermissionDto.getRoleId());
        Long roleId = assignPermissionDto.getRoleId();
        if (roleId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role id can not be null");
        }
        if (assignPermissionDto.getPermissionId() == null || assignPermissionDto.getPermissionId()
                .isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the set permission can not be null");
        }
        Role role = roleDaoService.findById(roleId);
        if (role == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role id: %d is not found", roleId));
        }
        //todo assign permission
        assignPermission(roleId, assignPermissionDto.getPermissionId());

    }

    @Override
    public void addRolePermission(AssignPermissionDto assignPermissionDto) {
        log.debug("assign the permission to the role ,{}", assignPermissionDto.getRoleId());
        Long roleId = assignPermissionDto.getRoleId();
        if (roleId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role id can not be null");
        }
        if (assignPermissionDto.getPermissionId() == null || assignPermissionDto.getPermissionId()
                .isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the set permission can not be null");
        }
        Role role = roleDaoService.findById(roleId);
        if (role == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role id: %d is not found", roleId));
        }
        //todo assign permission
        addPermission(roleId, assignPermissionDto.getPermissionId());
    }

    @Override
    public void removePermission(RemovePermissionDto removePermissionDto) {
        log.debug("remove the permission to the role {}:{}", removePermissionDto.getRoleId(),
                removePermissionDto.getRemovePermission());
        Long roleId = removePermissionDto.getRoleId();
        if (roleId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role id can not be null");
        }
        if (removePermissionDto.getRemovePermission() == null
                || removePermissionDto.getRemovePermission()
                .isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remove permission can not be null");
        }
        Role role = roleDaoService.findById(roleId);
        if (role == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role id: %d is not found", roleId));
        }
        //todo assign permission
        _removePermission(roleId, removePermissionDto.getRemovePermission());
    }

    private void _removePermission(Long roleId, List<Long> removePermissionId) {
        permissionDaoService.removeRolePermission(roleId, removePermissionId);
        AsynchronousExecutor.execute(permissionRoleLoadHandler::loadThePermissionRole);
    }

    private void addPermission(Long roleId, List<Long> permissionId) {
        permissionDaoService.addPermissionDto(roleId, permissionId);
        AsynchronousExecutor.execute(permissionRoleLoadHandler::loadThePermissionRole);
    }

    private void assignPermission(Long roleId, List<Long> permissionId) {
        //update the cache for the redis
        permissionDaoService.assignPermissionDto(roleId, permissionId);
        AsynchronousExecutor.execute(permissionRoleLoadHandler::loadThePermissionRole);
    }
}
