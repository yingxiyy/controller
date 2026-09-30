package net.flex.dci.otn.controller.subnet.manager.dto.view;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.subnet.manager.enums.SubnetMigrationType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ViewLinkMigrationDto implements Serializable {

    private String viewLinkId;

    private String subnetId;

    private String subnetName;

    private List<String> connectionIds;

    private SubnetMigrationType migrationType;

    private ViewLinkType viewLinkType;

    private String sourceSite;

    private String destSite;
}
