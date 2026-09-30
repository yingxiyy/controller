package net.flex.dci.otn.controller.subnet.manager.dto;

import java.io.Serializable;
import java.util.List;
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
@AllArgsConstructor
@NoArgsConstructor
public class MigrationTunnelInfo implements Serializable {

    private List<MigrationTunnel> migration;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MigrationTunnel {

        private String tunnelId;

        private String targetSubnet;
    }
}
