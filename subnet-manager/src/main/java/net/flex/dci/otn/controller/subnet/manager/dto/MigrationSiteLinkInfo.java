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
public class MigrationSiteLinkInfo implements Serializable {

    private List<MigrationSiteLink> migration;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MigrationSiteLink implements Serializable {

        private String siteLinkId;

        private String targetSubnet;
    }
}
