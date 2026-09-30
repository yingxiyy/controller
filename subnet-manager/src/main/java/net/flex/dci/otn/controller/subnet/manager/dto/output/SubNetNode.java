package net.flex.dci.otn.controller.subnet.manager.dto.output;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubNetNode implements Serializable {

    private String subnetId;

    private String name;

    private String parentId;

    private int level;

    private Integer order;

    private List<String> parentPathIds;

    private List<String> parentPathNames;

    private String fullPathName;

    private String fullPathIds;

    private Boolean hasChildren;

    private List<SubNetNode> children;
}
