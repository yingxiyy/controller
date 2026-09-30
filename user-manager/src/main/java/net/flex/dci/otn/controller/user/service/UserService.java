package net.flex.dci.otn.controller.user.service;

import java.util.List;
import net.flex.dci.otn.controller.user.domain.PageQueryParamDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateUserDto;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyPasswordDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyRoleDto;
import net.flex.dci.otn.controller.user.domain.rest.paged.UserPagedDto;

/**
 * @version 1.0
 * @date 2022/4/19 14:22
 */
public interface UserService {

    UserDto createUser(CreateUserDto createUserDto);

    void modifyUserPassword(ModifyPasswordDto passwordDto);

    void modifyUserRole(ModifyRoleDto roleDto);

    List<UserDto> listAllUser();

    UserPagedDto listAllAliveUserPaged(
            PageQueryParamDto pageQueryParamDto);

    UserDto getUserDetailInfoById(Long id);

    void deleteUserById(Long id);

    UserPagedDto listAllDeletedUserPaged(PageQueryParamDto pageQueryParamDto);
}
