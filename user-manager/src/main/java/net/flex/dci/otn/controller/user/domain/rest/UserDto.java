package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/19 13:46
 */
@Data
@Builder
@AllArgsConstructor
public class UserDto implements Serializable {

    private Long id;

    private String username;

    private String password;

    @JSONField(name = "create-time")
    private Date createTime;

    private String email;

    @JSONField(name = "role-name")
    private String roleName;

    @JSONField(name = "role-code")
    private String roleCode;

    private Long roleId;

    @JSONField(name = "modify-time")
    private Date modifyTime;

    @JSONField(name = "delete-time")
    private Date deleteTime;

}
