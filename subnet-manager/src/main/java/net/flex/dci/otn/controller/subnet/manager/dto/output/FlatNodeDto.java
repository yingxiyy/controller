package net.flex.dci.otn.controller.subnet.manager.dto.output;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/12
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlatNodeDto implements Serializable {

    private String id;

    private String subNetId;

    private String name;

    private Integer level;

    private Integer order;

    private boolean isLeaf;

    private String parentId;

    private String parentName;

    private String fullPath;

    private boolean hasChildren;

    private Long createTime;


    private Long updateTime;


    private String createBy;

    private String updateBy;
}
