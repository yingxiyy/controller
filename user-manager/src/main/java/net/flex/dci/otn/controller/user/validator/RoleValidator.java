package net.flex.dci.otn.controller.user.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/21 16:50
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RoleValidator {

    private final RoleDaoService roleDaoService;

    public void validateCreateRoleDto(RoleDto roleDto) {
        if (roleDto.getRoleName() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role name can not be null");
        }
        if (roleDto.getRoleCode() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role code can not be null");
        }
        String roleName = roleDto.getRoleName();
        String roleCode = roleDto.getRoleCode();
        boolean existsRoleName = roleDaoService.existsByRoleName(roleName);
        if (existsRoleName) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role name: %s is already existed", roleName));
        }
        boolean existsRoleCode = roleDaoService.existsByRoleCode(roleCode);
        if (existsRoleCode) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role code: %s is already existed", roleCode));
        }
    }

    public void validateRoleId(Long id) {
        if (!roleDaoService.existsById(id)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("can not find the role id is :{}", id));
        }
    }

}
