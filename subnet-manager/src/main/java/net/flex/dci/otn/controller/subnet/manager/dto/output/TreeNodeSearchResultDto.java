package net.flex.dci.otn.controller.subnet.manager.dto.output;

import java.io.Serializable;
import java.util.List;
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
public class TreeNodeSearchResultDto implements Serializable {

    private String subNetId;

    private String name;

    private Integer level;

    private String parentId;

    private String fullPath;

    private List<String> pathIds;

    private List<String> pathNames;
}
