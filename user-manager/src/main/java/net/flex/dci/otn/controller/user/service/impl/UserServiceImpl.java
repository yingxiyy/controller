package net.flex.dci.otn.controller.user.service.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.user.domain.PageQueryParamDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateUserDto;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyPasswordDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyRoleDto;
import net.flex.dci.otn.controller.user.domain.rest.paged.UserPagedDto;
import net.flex.dci.otn.controller.user.message.UaChangeMessage;
import net.flex.dci.otn.controller.user.service.UserService;
import net.flex.dci.otn.controller.user.utils.UserRoleConvertor;
import net.flex.dci.otn.controller.user.validator.UserValidator;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.entity.security.User;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import net.flex.dci.otn.db.jpa.service.dao.UserDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.DeletedUserDetailInfoDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.UserDetailDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.UserDetailInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/4/19 14:23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserValidator userValidator;

    private final UserDaoService userDaoService;

    private final RoleDaoService roleDaoService;

    private final UaChangeMessage uaChangeMessage;

    @Override
    public UserDto createUser(CreateUserDto createUserDto) {
        log.debug("start to create user, user name :{}", createUserDto.getUsername());
        userValidator.validateCreateUser(createUserDto);
        User user = new User();
        user.setCreateTimestamp(new Timestamp(new Date().getTime()));
        user.setUsername(createUserDto.getUsername());
        String password = passwordEncoder.encode(createUserDto.getPassword());
        user.setPassword(password);
        user.setEmail(createUserDto.getEmail());

        Role role = new Role();
        role.setId(createUserDto.getRoleDto().getRoleId());

        User saveUser = userDaoService.createUser(user, role);

        UserDto userDto = UserDto.builder().id(saveUser.getId())
                .createTime(saveUser.getCreateTimestamp())
                .build();
        uaChangeMessage.notifyUserCreate(user, createUserDto.getRoleDto().getRoleId());
        return userDto;
    }

    @Override
    public void modifyUserPassword(ModifyPasswordDto passwordDto) {
        log.info("modify the user password,the userId is :{}", passwordDto.getUserId());
        //todo:validate the code if needed
        userValidator.validateModifyPassword(passwordDto);
        String password = passwordEncoder.encode(passwordDto.getPassword());
        Long userId = passwordDto.getUserId();
        User updateUser = userDaoService.modifyTheUserPassword(userId, password);
        if (updateUser != null) {
            log.debug("success to modify the user password");
        }
    }

    @Override
    public void modifyUserRole(ModifyRoleDto roleDto) {
        log.info("modify the user password,the userId is :{}", roleDto.getUserId());
        //todo:validate the code if needed
        userValidator.validateModifyRoleDto(roleDto);

        Long userId = roleDto.getUserId();
        Long roleId = roleDto.getRoleId();
        int result = userDaoService.modifyTheUserRole(userId, roleId);
        if (result > 0) {
            log.debug("success to modify the user password");
        }

        uaChangeMessage.notifyUserUpdateRole(userId, roleId);

    }

    @Override
    public List<UserDto> listAllUser() {
        return null;
    }

    @Override
    public UserPagedDto listAllAliveUserPaged(PageQueryParamDto pageQueryParamDto) {
        log.debug("list all user paged by query dto:{},offset is :{},limit is :{}",
                pageQueryParamDto, pageQueryParamDto.getPage(), pageQueryParamDto.getLimit());
        Page<UserDetailInfoDto> userPage = userDaoService.listAllAliveUserPagedByCondition(
                pageQueryParamDto.getPage(),
                pageQueryParamDto.getLimit(),
                pageQueryParamDto.getDirection(),
                pageQueryParamDto.getKeywords(),
                pageQueryParamDto.getOrderElement().getElementName());

        UserPagedDto userPagedDto = UserRoleConvertor.convertUserEntityPaged2Dto(userPage);
        return userPagedDto;
    }

    @Override
    public UserDto getUserDetailInfoById(Long id) {
        log.debug("get user detail info by id:{}", id);
        if (null == id) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the user id should not be null");
        }
        userValidator.validateUserId(id);
        UserDetailDto userDetailDto = userDaoService.loadByUserId(id);
        UserDto userDto = UserRoleConvertor.convertUserDetailDto2UserDto(userDetailDto);
        return userDto;
    }

    @Override
    public void deleteUserById(Long id) {
        log.debug("delete user by id:{}", id);
        if (null == id) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the user id should not be null");
        }
        userValidator.validateUserId(id);
        userDaoService.deleteUserById(id);
        uaChangeMessage.notifyUserDelete(id);
    }

    @Override
    public UserPagedDto listAllDeletedUserPaged(PageQueryParamDto pageQueryParamDto) {
        log.debug("list all user paged by query dto:{},offset is :{},limit is :{}",
                pageQueryParamDto, pageQueryParamDto.getPage(), pageQueryParamDto.getLimit());
        Page<DeletedUserDetailInfoDto> userPage = userDaoService.listAllDeletedUserPagedByCondition(
                pageQueryParamDto.getPage(),
                pageQueryParamDto.getLimit(),
                pageQueryParamDto.getDirection(),
                pageQueryParamDto.getKeywords(),
                pageQueryParamDto.getOrderElement().getElementName());

        UserPagedDto userPagedDto = UserRoleConvertor.convertDeleteUserEntityPaged2Dto(userPage);
        return userPagedDto;
    }
}
