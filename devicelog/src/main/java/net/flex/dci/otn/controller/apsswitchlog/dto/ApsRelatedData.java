package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApsRelatedData implements Serializable {

    public String apsTpId;

    public static final int TUNNEL = 0;
    public static final int SITELINK = 1;

    private int type = TUNNEL;
    private List<ApsRelatedTunnelOrSiteLink> data;
}
