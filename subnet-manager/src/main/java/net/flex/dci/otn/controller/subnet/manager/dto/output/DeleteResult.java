package net.flex.dci.otn.controller.subnet.manager.dto.output;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/13
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteResult implements Serializable {

    private String subnetId;

    private String name;

    private boolean isLeaf;

    private boolean isCanDelete = true;

    private boolean success;

    private boolean selfDeleted;

    private int totalDeleted;

    private int deleteDescendantCount;

    private String message;

    private List<String> deletedNodeIds;
    private List<String> deletedDescendantIds;
}
