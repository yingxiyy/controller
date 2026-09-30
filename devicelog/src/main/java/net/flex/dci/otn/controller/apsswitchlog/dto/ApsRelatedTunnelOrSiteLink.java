package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApsRelatedTunnelOrSiteLink implements Serializable {

    private String id;
    private String name;
}
