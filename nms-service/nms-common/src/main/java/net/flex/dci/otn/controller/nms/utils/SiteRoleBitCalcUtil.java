package net.flex.dci.otn.controller.nms.utils;

import java.util.List;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.nms.enums.SiteRole;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;

/**
 * 2026/3/16
 *
 * @author musa
 * @version 1.0
 **/
public class SiteRoleBitCalcUtil {

    public static SiteType calcByNeSubType(List<NeSubType> neSubTypes) {
        if (neSubTypes.isEmpty()) {
            return SiteType.SITE;
        }
        int bitResult = 0;
        for (NeSubType neSubType : neSubTypes) {
            bitResult |= SiteRole.fromNeSubType(neSubType).getBitCode();
        }
        if ((bitResult & SiteRole.OTM.getBitCode()) != 0) {
            return SiteRole.OTM.getSiteType();
        } else if ((bitResult & SiteRole.ROADM.getBitCode()) != 0) {
            return SiteRole.ROADM.getSiteType();
        } else if ((bitResult & SiteRole.SITE.getBitCode()) != 0) {
            return SiteRole.SITE.getSiteType();
        } else if ((bitResult & SiteRole.ILA.getBitCode()) != 0) {
            return SiteRole.ILA.getSiteType();
        } else if ((bitResult & SiteRole.DGE.getBitCode()) != 0) {
            return SiteRole.DGE.getSiteType();
        }

        return SiteType.SITE;
    }
}
