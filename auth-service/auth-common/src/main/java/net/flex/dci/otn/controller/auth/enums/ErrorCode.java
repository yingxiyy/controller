package net.flex.dci.otn.controller.auth.enums;

/**
 * @version 1.0
 * @date 2022/4/25 12:59
 */
public enum ErrorCode implements IErrorCode {
    ERROR_USER_NAME_EXIST(104101, "user is already existed"),
    ERROR_USER_NOT_EXIST(104102, "user is not existed"),
    ERROR_USER_PASSWORD_INCORRECT(104003, "user account or password is invalided"),
    ERROR_USER_LOCKED(104104, "the user is locked"),
    ERROR_USER_EXPIRE(104105, "the user is expired");

    private final int code;

    private final String msg;

    private ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
