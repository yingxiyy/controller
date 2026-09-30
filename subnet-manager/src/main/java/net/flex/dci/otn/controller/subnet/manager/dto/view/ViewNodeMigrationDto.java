package net.flex.dci.otn.controller.subnet.manager.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.subnet.manager.enums.SubnetMigrationType;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewNodeMigrationDto {

    private String viewNodeId;

    private String subnetId;

    private String subnetName;

    private Integer subnetLevel;

    private List<String> phyNodeId;

    private SubnetMigrationType migrationType;
}
