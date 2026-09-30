package net.flex.dci.otn.controller.subnet.manager.dto.task;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/5/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubnetMigrateTunnelInfo implements Serializable {

    private String tunnelName;

    private String tunnelId;

    private String oldSubnetName;

    private String oldSubnetId;

    private String targetSubnetName;

    private String targetSubnetId;


}
