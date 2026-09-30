package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;

/** Selects the Bone2.0 Flex C-band card chain without changing legacy site models. */
final class ByteDance2CardChainResolver {

    private static final int FLEX_GRID = 0;
    private static final int FIXED_75_GRID = 75;
    private static final int FIXED_150_GRID = 150;
    private static final int BANDWIDTH_64 = 64;
    private static final String CMUX_CARD_CLASS = "CMUX";
    private static final String LEGACY_OA_CARD_CLASS = "OA";
    private static final String TILA_CARD_CLASS = "ILA";
    private static final String OTM_NODE_TYPE = "T";
    private static final String MUX_CARD_CLASS = "MUX";
    private static final String LEGACY_MUX64_CARD_CLASS = "MUX64";

    private ByteDance2CardChainResolver() {
    }

    static List<String> resolve(String vendorName, String productType, WDM_Band wdmBand, int grid,
            Integer bandwidth, List<String> originalCardClasses) {
        if (!ProductTypeResolver.isBone20ProductType(vendorName, productType)
                || wdmBand != WDM_Band.C || grid != FLEX_GRID) {
            return originalCardClasses;
        }

        // Bone2.0 replaces the legacy OA with TILA. Both OTM and ROADM add/drop
        // layouts use two FMUX_32 cards at 64 channels.
        List<String> resolved = new ArrayList<>(originalCardClasses);
        Collections.replaceAll(resolved, LEGACY_OA_CARD_CLASS, TILA_CARD_CLASS);
        int fmuxIndex = resolved.indexOf(CMUX_CARD_CLASS);
        if ((bandwidth == null || bandwidth == BANDWIDTH_64) && fmuxIndex >= 0
                && Collections.frequency(resolved, CMUX_CARD_CLASS) == 1) {
            resolved.add(fmuxIndex + 1, CMUX_CARD_CLASS);
        }
        return resolved;
    }

    static List<String> resolveFixedOtm(String vendorName, String productType, WDM_Band wdmBand, int grid,
            boolean protectedLink, String nodeType, List<String> originalCardClasses) {
        if (!ProductTypeResolver.isBone20ProductType(vendorName, productType)
                || wdmBand != WDM_Band.C || !OTM_NODE_TYPE.equals(nodeType)
                || (grid != FIXED_75_GRID && grid != FIXED_150_GRID)) {
            return originalCardClasses;
        }
        // Reuse the fixed-grid siteModel chain and only remap legacy card classes to Bone2.0 classes.
        List<String> resolved = new ArrayList<>(originalCardClasses);
        Collections.replaceAll(resolved, LEGACY_MUX64_CARD_CLASS, MUX_CARD_CLASS);
        Collections.replaceAll(resolved, LEGACY_OA_CARD_CLASS, TILA_CARD_CLASS);
        return resolved;
    }

    static List<String> resolveProtectedPeers(String vendorName, String productType,
            List<String> originalCardClasses) {
        if (!ProductTypeResolver.isBone20ProductType(vendorName, productType)) {
            return originalCardClasses;
        }
        List<String> resolved = new ArrayList<>(originalCardClasses);
        Collections.replaceAll(resolved, LEGACY_OA_CARD_CLASS, TILA_CARD_CLASS);
        return resolved;
    }

    static String getFmuxMainLinePort(boolean protectedLink) {
        return protectedLink ? "SIGA" : "SIG";
    }
}
