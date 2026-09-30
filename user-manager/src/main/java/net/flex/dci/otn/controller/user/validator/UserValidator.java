package net.flex.dci.otn.controller.user.validator;

import static net.flex.dci.otc.common.constants.AuthConstant.DEFAULT_USER_NAME;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.user.domain.rest.CreateUserDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyPasswordDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyRoleDto;
import net.flex.dci.otn.db.jpa.entity.security.User;
import net.flex.dci.otn.db.jpa.service.dao.RoleDaoService;
import net.flex.dci.otn.db.jpa.service.dao.UserDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/19 15:41
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserValidator {

    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private final UserDaoService userDaoService;
    private final RoleDaoService roleDaoService;

    /**
     * validate Create User dto
     *
     * @param createUserDto
     */
    public void validateCreateUser(CreateUserDto createUserDto) {
        log.debug("validate create user ");
        //validate username is exists
        String username = createUserDto.getUsername();
        String email = createUserDto.getEmail();
        if (username == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the username should be not null");
        }
        if (username.equals(DEFAULT_USER_NAME)) {
            throw new CommonException(CommonExceptionType.EXISTED_ERROR,
                    String.format("the username: %s is already existed ", username));
        }
        User user = userDaoService.findByUsername(username);
        if (user != null) {
            throw new CommonException(CommonExceptionType.EXISTED_ERROR,
                    String.format("the username: %s is already existed ", username));
        }
        /**
         * email is validate
         */
        if (email != null) {
            boolean valid = isValidMail(email);
            if (!valid) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("the email: %s is invalided", email));
            }
            User emailRefUser = userDaoService.findByEmail(email);
            if (emailRefUser != null) {
                throw new CommonException(CommonExceptionType.EXISTED_ERROR,
                        String.format("the email: %s is already existed ", email));
            }
        }

    }

    private boolean isValidMail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.find();
    }

    public void validateModifyPassword(ModifyPasswordDto passwordDto) {
        if (passwordDto.getUserId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the userId should not be null");
        }
        if (null == passwordDto.getPassword()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the password should not be null");
        }
        User user = userDaoService.findOne(passwordDto.getUserId());
        if (user == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("can not found the user id : %s  ", passwordDto.getUserId()));
        }
    }

    public void validateModifyRoleDto(ModifyRoleDto roleDto) {
        Long userId = roleDto.getUserId();
        Long roleId = roleDto.getRoleId();
        if (null == roleDto.getUserId()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the user should not be null");
        }
        if (null == roleDto.getRoleId()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the role should not be null");
        }
        if (!userDaoService.existsById(userId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the user id :%d is not found", userId));
        }
        if (!roleDaoService.existsById(roleId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the role id :%d is not found", userId));
        }

    }

    public void validateUserId(Long id) {
        log.debug("start to validate the user id:{}", id);
        Boolean existed = userDaoService.existsById(id);
        if (!existed) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the user id :%d is not found ", id));
        }
    }
}
