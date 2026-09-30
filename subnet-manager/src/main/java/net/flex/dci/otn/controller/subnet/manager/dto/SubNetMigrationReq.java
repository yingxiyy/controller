package net.flex.dci.otn.controller.subnet.manager.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubNetMigrationReq implements Serializable {

    private MigrationSiteLinkInfo migrationSiteLink;

    private MigrationTunnelInfo migrationTunnel;
}
