package net.flex.dci.otn.controller.allocate.designer.tunnel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

class OchpResourceLayoutFactoryTest {

    @Test
    void defaultLayoutKeepsCommercialCSelectionUnchanged() {
        OchpResourceLayout layout = OchpResourceLayoutFactory.create("COHERENT", "CHASSIS", null);

        assertTrue(layout.allowsMdPort("M1D1"));
        assertTrue(layout.allowsMdPort("M64D64"));
    }

    @Test
    void byteDance2Bandwidth32RejectsSecondFmuxChannels() {
        OchpResourceLayout layout = OchpResourceLayoutFactory.create("COHERENT", "CHASSIS2.0", 32);

        assertTrue(layout.allowsMdPort("M1D1"));
        assertTrue(layout.allowsMdPort("M32D32"));
        assertFalse(layout.allowsMdPort("M33D33"));
    }

    @Test
    void eachSiteLinkUsesItsOwnBandwidth() {
        OchpResourceLayout bandwidth32 = OchpResourceLayoutFactory.create("COHERENT", "CHASSIS2.0", 32);
        OchpResourceLayout bandwidth64 = OchpResourceLayoutFactory.create("COHERENT", "CHASSIS2.0", 64);

        assertFalse(bandwidth32.allowsMdPort("M33D33"));
        assertTrue(bandwidth64.allowsMdPort("M33D33"));
        assertTrue(bandwidth64.allowsMdPort("M64D64"));
    }

    @Test
    void factoryUsesEachSiteLinksInitialCapacityInsteadOfRemainingBandwidth() {
        OchpResourceLayout bandwidth32 = OchpResourceLayoutFactory.create(siteLink(32, "31"));
        OchpResourceLayout bandwidth64 = OchpResourceLayoutFactory.create(siteLink(64, "31"));

        assertFalse(bandwidth32.allowsMdPort("M33D33"));
        assertTrue(bandwidth64.allowsMdPort("M33D33"));
    }

    private Link siteLink(int resourceBandwidth, String remainingBandwidth) {
        Properties properties = new PropertiesBuilder().setProperty(java.util.Collections.singletonList(
                        new PropertyBuilder().setName("resource-bandwidth")
                                .setValue(String.valueOf(resourceBandwidth)).build())).build();
        Link1 augmentation = new Link1Builder().setSite(new SiteBuilder()
                .setVendorName("COHERENT").setProductType("CHASSIS2.0")
                .setBandwidth(remainingBandwidth).setProperties(properties).build()).build();
        return new LinkBuilder().addAugmentation(Link1.class, augmentation).build();
    }
}
