package net.flex.dci.otn.controller.allocate.designer.tunnel;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

/** Selects the OCHP layout independently for each siteLink route leg. */
final class OchpResourceLayoutFactory {

    static final String RESOURCE_BANDWIDTH_PROPERTY = "resource-bandwidth";

    private OchpResourceLayoutFactory() {
    }

    static OchpResourceLayout create(String vendorName, String productType, Integer bandwidth) {
        if (!ProductTypeResolver.isBone20ProductType(vendorName, productType)) {
            return new DefaultOchpResourceLayout();
        }
        return new ByteDance2OchpResourceLayout(bandwidth);
    }

    static OchpResourceLayout create(Link siteLink) {
        Site site = siteLink.getAugmentation(Link1.class).getSite();
        if (!ProductTypeResolver.isBone20ProductType(site.getVendorName(), site.getProductType())) {
            return new DefaultOchpResourceLayout();
        }

        String configuredBandwidth = PropertyTool.getValue(site.getProperties(),
                RESOURCE_BANDWIDTH_PROPERTY);
        if (configuredBandwidth == null || configuredBandwidth.isEmpty()) {
            // Compatibility for Bone2.0 siteLinks created before resource-bandwidth was persisted.
            configuredBandwidth = site.getBandwidth();
        }
        int bandwidth = Integer.parseInt(configuredBandwidth);
        if (bandwidth != 32 && bandwidth != 64) {
            // Legacy links only stored the remaining counter. Values above 32 can only be 64-wave.
            bandwidth = bandwidth > 32 ? 64 : 32;
        }
        return new ByteDance2OchpResourceLayout(bandwidth);
    }
}
