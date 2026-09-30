package net.flex.dci.otn.controller.subnet.manager.component.view;

import java.util.List;
import java.util.Map;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewTopologyMigrationDto;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
public interface ViewTopologyMigration {

    void migrate(List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList);

    void migrate(Map<String, String> nodeOldSubnetMap, String subnetId);
}
