package net.flex.dci.otn.controller.subnet.manager.component.migration;

import java.util.List;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
public interface SubNetMigration {

    void reassignmentSubnet(String subnetId, List<String> siteLinkIds, List<String> tunnelIds,
            String author);

    void reassignmentSubnet(MigrationSiteLinkInfo migrationSiteLink,
            MigrationTunnelInfo migrationTunnel, String author);
}
