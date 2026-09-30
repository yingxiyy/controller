package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/22 16:31
 */
@Data
public class CreateOrUpdatePermissionDto implements Serializable {

    private String component;

    private String path;

    private String url;

    @JSONField(name = "method")
    private String httpMethod;

    private String name;

    private String description;

    private Long id;
}
