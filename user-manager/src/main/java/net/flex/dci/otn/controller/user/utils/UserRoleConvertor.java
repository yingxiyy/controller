package net.flex.dci.otn.controller.user.utils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.domain.PermissionDto;
import net.flex.dci.otn.controller.user.domain.RolePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateOrUpdatePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;
import net.flex.dci.otn.controller.user.domain.rest.paged.UserPagedDto;
import net.flex.dci.otn.db.jpa.entity.security.Permission;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.entity.security.User;
import net.flex.dci.otn.db.jpa.service.dao.dto.DeletedUserDetailInfoDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.UserDetailDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.UserDetailInfoDto;
import org.springframework.data.domain.Page;

/**
 * @version 1.0
 * @date 2022/4/21 14:32
 */
@Slf4j
public class UserRoleConvertor {

    public static List<RoleDto> convertRoleEntity2DtoList(List<Role> roles) {
        return roles.stream().map(UserRoleConvertor::convertRoleEntity2Dto)
                .collect(Collectors.toList());
    }

    public static RoleDto convertRoleEntity2Dto(Role role) {
        return RoleDto.builder().roleId(role.getId()).roleCode(role.getRoleCode())
                .roleName(role.getName()).build();
    }


    public static UserPagedDto convertUserEntityPaged2Dto(Page<UserDetailInfoDto> userPage) {

        return UserPagedDto.builder().userDtos(
                        userPage.get().map(UserRoleConvertor::convertUserDetailsDto2UserDto)
                                .collect(Collectors.toList()))
                .currentPage(userPage.getPageable().getPageNumber() + 1)
                .totalPage(userPage.getTotalPages())
                .totalUsers(userPage.getTotalElements())
                .build();
    }

    public static UserDto convertUserDetailsDto2UserDto(UserDetailInfoDto userDetailInfoDto) {
        return UserDto.builder()
                .username(userDetailInfoDto.getUsername())
                .createTime(userDetailInfoDto.getCreateTimestamp())
                .email(userDetailInfoDto.getEmail())
                .roleCode(userDetailInfoDto.getRoleCode())
                .roleName(userDetailInfoDto.getRoleName())
                .id(userDetailInfoDto.getId())
                .roleId(userDetailInfoDto.getRoleId())
                .build();
    }

    public static UserDto convertUserDetailDto2UserDto(UserDetailDto userDetailDto) {
        User user = userDetailDto.getUser();
        Role role = userDetailDto.getRole();
        return UserDto.builder()
                .username(user.getUsername())
                .createTime(user.getCreateTimestamp())
                .email(user.getEmail())
                .roleCode(role.getRoleCode())
                .roleName(role.getName())
                .id(user.getId())
                .roleId(role.getId())
                .modifyTime(user.getModifyTimestamp()
                )
                .build();
    }

    public static UserPagedDto convertDeleteUserEntityPaged2Dto(
            Page<DeletedUserDetailInfoDto> userPage) {
        return UserPagedDto.builder().userDtos(
                        userPage.get().map(UserRoleConvertor::convertDeleteUserDetailsDto2UserDto)
                                .collect(Collectors.toList()))
                .currentPage(userPage.getPageable().getPageNumber() + 1)
                .totalPage(userPage.getTotalPages())
                .totalUsers(userPage.getTotalElements())
                .build();
    }

    public static UserDto convertDeleteUserDetailsDto2UserDto(
            DeletedUserDetailInfoDto user) {
        return UserDto.builder()
                .username(user.getUsername())
                .createTime(user.getCreateTimestamp())
                .email(user.getEmail())
                .roleCode(user.getRoleCode())
                .roleName(user.getRoleName())
                .id(user.getId())
                .roleId(user.getId())
                .deleteTime(user.getDeleteTimestamp()
                )
                .build();
    }

    public static List<PermissionDto> convertPermissionEntities2Dtos(List<Permission> permissions) {
        return permissions.stream().map(UserRoleConvertor::convertPermissionEntity2Dto)
                .collect(Collectors.toList());
    }


    public static PermissionDto convertPermissionEntity2Dto(Permission permission) {
        return PermissionDto.builder().id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .path(permission.getPath())
                .type(permission.getType())
                .url(permission.getUrl())
                .component(permission.getComponent())
                .enable(permission.getEnabled())
                .build();
    }

    public static Permission convertCreatePermissionDtoToEntity(
            CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        Permission permission = new Permission();
        permission.setId(
                createOrUpdatePermissionDto.getId() != null ? createOrUpdatePermissionDto.getId()
                        : null);
        permission.setComponent(createOrUpdatePermissionDto.getComponent());
        permission.setDescription(createOrUpdatePermissionDto.getDescription());
        permission.setName(createOrUpdatePermissionDto.getName());
        permission.setPath(createOrUpdatePermissionDto.getPath());
        permission.setType(createOrUpdatePermissionDto.getHttpMethod());
        permission.setUrl(createOrUpdatePermissionDto.getUrl());
        return permission;
    }

    public static RolePermissionDto convertRolePermissions2Dto(Role role,
            List<Permission> permissions) {
        return RolePermissionDto.builder()
                .roleId(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getName())
                .permissionDtos(convertPermissionEntities2Dtos(permissions))
                .build();
    }
}
