package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/24 15:03
 */
@Data
@Builder
@AllArgsConstructor
public class AssignPermissionDto implements Serializable {

    @JSONField(name = "role-id")
    private Long roleId;

    @JSONField(name = "permissions")
    private List<Long> permissionId;
}
