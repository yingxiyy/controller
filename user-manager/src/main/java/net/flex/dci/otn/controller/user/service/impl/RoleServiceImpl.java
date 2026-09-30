package net.flex.dci.otn.controller.user.service.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;
import net.flex.dci.otn.controller.user.message.UaChangeMessage;
import net.flex.dci.otn.controller.user.service.RoleService;
import net.flex.dci.otn.controller.user.utils.UserRoleConvertor;
import net.flex.dci.otn.controller.user.validator.RoleValidator;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/4/19 14:23
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleDaoService roleDaoService;

    private final RoleValidator roleValidator;

    private final UaChangeMessage uaChangeMessage;

    @Override
    public List<RoleDto> listAllRoles() {
        log.debug("list all roles");
        List<Role> roles = roleDaoService.findAll();
        List<RoleDto> roleDtos = UserRoleConvertor.convertRoleEntity2DtoList(roles);
        return roleDtos;
    }

    @Override
    public void createRole(RoleDto roleDto) {
        log.debug("create role,role is :{}", roleDto);
        roleValidator.validateCreateRoleDto(roleDto);
        Role role = new Role();
        role.setName(roleDto.getRoleName());
        role.setDescription(roleDto.getRoleDescription());
        role.setRoleCode(roleDto.getRoleCode());
        role.setCreateTime(new Timestamp(new Date().getTime()));
        Role saveRole = roleDaoService.save(role);
        uaChangeMessage.notifyRoleCreate(saveRole);
    }

    @Override
    public void deleteRole(Long id) {
        log.debug("delete role,role id is :{}", id);
        roleValidator.validateRoleId(id);
        roleDaoService.deleteRoleById(id);
        uaChangeMessage.notifyRoleDelete(id);
    }
}
