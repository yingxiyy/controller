package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Arrays;
import java.util.List;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;

/** Selects a vendor layout strategy while leaving all ordinary requests on the default path. */
final class SiteResourceLayoutFactory {

    private static final String BONE20_ONE_TO_TWO_LINK_MODEL = "10";
    private static final List<LayoutSelector> SELECTORS = Arrays.asList(
            new ByteDance2Flex32UnprotectedLayoutSelector(),
            new ByteDance2Flex64LayoutSelector(),
            new ByteDance2OneToTwoLayoutSelector());

    private SiteResourceLayoutFactory() {
    }

    static SiteResourceLayout create(SiteInput input) {
        for (LayoutSelector selector : SELECTORS) {
            if (selector.supports(input)) {
                return selector.create(input);
            }
        }
        if (isByteDance2(input)) {
            return new ByteDance2DefaultResourceLayout();
        }
        return new DefaultSiteResourceLayout();
    }

    private static boolean isByteDance2(SiteInput input) {
        return input != null
                && ProductTypeResolver.isBone20ProductType(input.getVendorName(), input.getVendorType());
    }

    private interface LayoutSelector {
        boolean supports(SiteInput input);

        SiteResourceLayout create(SiteInput input);
    }

    private static final class ByteDance2Flex32UnprotectedLayoutSelector
            implements LayoutSelector {

        @Override
        public boolean supports(SiteInput input) {
            return input != null
                    && ProductTypeResolver.isBone20ProductType(
                            input.getVendorName(), input.getVendorType())
                    && input.getWdmBand() == WDM_Band.C
                    && input.getGrid() == 0
                    && Integer.valueOf(32).equals(input.getBandwidth())
                    && ProtectionUnprotected.class.equals(input.getProtectionType());
        }

        @Override
        public SiteResourceLayout create(SiteInput input) {
            return new ByteDance2Flex32UnprotectedResourceLayout();
        }
    }

    private static final class ByteDance2Flex64LayoutSelector implements LayoutSelector {

        @Override
        public boolean supports(SiteInput input) {
            return input != null
                    && ProductTypeResolver.isBone20ProductType(input.getVendorName(), input.getVendorType())
                    && input.getWdmBand() == WDM_Band.C
                    && input.getGrid() == 0
                    && Integer.valueOf(64).equals(input.getBandwidth())
                    && (ProtectionUnprotected.class.equals(input.getProtectionType())
                    || ProtectionBidir1To1.class.equals(input.getProtectionType()));
        }

        @Override
        public SiteResourceLayout create(SiteInput input) {
            return new ByteDance2Flex64ResourceLayout(
                    ProtectionBidir1To1.class.equals(input.getProtectionType()));
        }
    }

    private static final class ByteDance2OneToTwoLayoutSelector implements LayoutSelector {

        @Override
        public boolean supports(SiteInput input) {
            return input != null
                    && ProductTypeResolver.isBone20ProductType(input.getVendorName(), input.getVendorType())
                    && input.getWdmBand() == WDM_Band.C
                    && (ProtectionBidir1To1.class.equals(input.getProtectionType())
                    || ProtectionBidir1To2.class.equals(input.getProtectionType()));
        }

        @Override
        public SiteResourceLayout create(SiteInput input) {
            if (input.getGrid() == 0) {
                if (ProtectionBidir1To1.class.equals(input.getProtectionType())) {
                    return new ByteDance2FlexOneToOneResourceLayout();
                }
                return new ByteDance2OneToTwoResourceLayout(input.getBandwidth());
            }
            // Fixed-grid OLP3-3 three-leg layout is explicit link-model 10 only.
            if (BONE20_ONE_TO_TWO_LINK_MODEL.equals(input.getLinkModel())
                    && (input.getGrid() == 75 || input.getGrid() == 150)) {
                return new ByteDance2FixedOneToTwoResourceLayout();
            }
            return new ByteDance2DefaultResourceLayout();
        }
    }
}
