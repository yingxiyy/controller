package net.flex.dci.otn.controller.subnet.manager.dto.task;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/2/24
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubnetMigrateSiteLinkInfo implements Serializable {

    private String siteLinkName;

    private String siteLinkId;

    private String oldSubnetName;

    private String oldSubnetId;

    private String targetSubnetId;

    private String targetSubnetName;

}
