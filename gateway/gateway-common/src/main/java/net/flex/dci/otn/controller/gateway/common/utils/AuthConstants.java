package net.flex.dci.otn.controller.gateway.common.utils;

/**
 * @version 1.0
 * @date 2022/4/18 10:48
 */
public class AuthConstants {

    public static final String AUTHORITY_CLAIM_NAME = "authorities";
    public static final String JWT_TOKEN_HEADER = "Authorization";

    public static final String SIGN_KEY = "dci-world";
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    public static final String USER_NAME_TOKEN_HEADER = "user";
    public static final String USER_WHO_TOKEN_HEADER = "who";
    public static final String USER_TOKEN_HEADER = "user-id";


    public static final String USER_ID = "user_id";

    public static final String USER_NAME = "sub";

    public static final String JWT_AUTHORITY_PREFIX = "ROLE_";

    public static final String JWT_PRODUCER = "auth-service";
}
