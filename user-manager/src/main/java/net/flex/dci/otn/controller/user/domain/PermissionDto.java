package net.flex.dci.otn.controller.user.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/24 10:04
 */
@Data
@Builder
@AllArgsConstructor
public class PermissionDto implements Serializable {

    private Long id;

    private String component;

    private String name;

    private String path;

    private String url;

    private String type;

    private String description;

    private Boolean enable;
}
