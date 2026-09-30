package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/10/20
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
public class RemovePermissionDto implements Serializable {

    @JSONField(name = "role-id")
    private Long roleId;

    @JSONField(name = "permissions")
    private List<Long> removePermission;
}
