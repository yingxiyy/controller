package net.flex.dci.otn.controller.implement.common.impl;

import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;

public final class SharedPhysicalLinkUsage {

    private SharedPhysicalLinkUsage() {
    }

    public static boolean isSharedByOchLinks(String linkId) {
        return PhysicalLinkIdNamingRule.isWssLink(linkId)
                || PhysicalLinkIdNamingRule.isOmsLink(linkId);
    }

    public static boolean shouldSkipOnDeimplement(String linkId, long otherNonAllocateOchCount) {
        // OS links are OT-L to MUX-MD connections on the current OCH path. They must be
        // released with this OCH, while WSS/OMS links can be reused by other OCHs.
        return isSharedByOchLinks(linkId) && otherNonAllocateOchCount > 0;
    }
}
