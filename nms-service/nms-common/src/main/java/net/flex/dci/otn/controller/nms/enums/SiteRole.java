package net.flex.dci.otn.controller.nms.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import net.flex.dci.otc.mongo.enums.NeSubType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;

/**
 * 2026/3/16
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum SiteRole {
    SITE(1 << 0, SiteType.SITE, Collections.EMPTY_LIST),
    REG(1 << 1, SiteType.REG, Collections.singletonList(NeSubType.EPC_REG)),
    DGE(1 << 2, SiteType.DGE, Collections.singletonList(NeSubType.OPC_DGE)),
    ILA(1 << 3, SiteType.ILA, Collections.singletonList(NeSubType.OPC_ILA)),

    ROADM(1 << 4, SiteType.ROADM, Collections.singletonList(NeSubType.OPC_ROADM)),
    OTM(1 << 5, SiteType.OTM, Arrays.asList(NeSubType.EPC_OTM, NeSubType.OPC_OTM));

    private final int bitCode;

    private final SiteType siteType;

    private final List<NeSubType> subTypes;

    SiteRole(int bitCode, SiteType siteType, List<NeSubType> subTypes) {
        this.bitCode = bitCode;
        this.siteType = siteType;
        this.subTypes = subTypes;
    }

    public static SiteRole fromBitCode(int bitCode) {

        for (SiteRole siteRole : SiteRole.values()) {
            if (siteRole.bitCode == bitCode) {
                return siteRole;
            }
        }
        throw new IllegalArgumentException("unknown site role bite code:" + bitCode);
    }

    public static SiteRole fromNeSubType(NeSubType neSubType) {
        for (SiteRole siteRole : SiteRole.values()) {
            if (siteRole.subTypes.contains(neSubType)) {
                return siteRole;
            }
        }
        throw new IllegalArgumentException("unknown neSubType:" + neSubType);
    }


}
