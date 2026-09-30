package net.flex.dci.otn.controller.implement.site.nbi.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

class CheckerTest {

    @Test
    void filtersByPersistedAseCapabilityInsteadOfBandAlone() {
        SiteLinkDao siteLinkDao = mock(SiteLinkDao.class);
        when(siteLinkDao.getSiteLinkById("legacy-cl"))
                .thenReturn(siteLink("legacy-cl", NeYangModel.ByteDance, WDM_Band.C_L, GridType._150));
        when(siteLinkDao.getSiteLinkById("commercial-c"))
                .thenReturn(siteLink("commercial-c", NeYangModel.ByteDance, WDM_Band.C, GridType._0));
        when(siteLinkDao.getSiteLinkById("bone-flex"))
                .thenReturn(siteLink("bone-flex", NeYangModel.Chassis20, WDM_Band.C, GridType._0));
        when(siteLinkDao.getSiteLinkById("bone-fixed"))
                .thenReturn(siteLink("bone-fixed", NeYangModel.Chassis20, WDM_Band.C, GridType._150));

        Checker checker = new Checker(siteLinkDao);

        assertEquals(Arrays.asList("legacy-cl", "bone-flex"),
                checker.filterAseSupportedSiteLinks(
                        Arrays.asList("legacy-cl", "commercial-c", "bone-flex", "bone-fixed")));
    }

    private Link siteLink(String id, NeYangModel yangModel, WDM_Band band, GridType grid) {
        return new LinkBuilder()
                .setLinkId(new LinkId(id))
                .addAugmentation(Link1.class, new Link1Builder()
                        .setSite(new SiteBuilder()
                                .setLinkGroup(band.toString())
                                .setGrid(grid)
                                .setProperties(new PropertiesBuilder()
                                        .setProperty(Collections.singletonList(new PropertyBuilder()
                                                .setName("yang-model")
                                                .setValue(yangModel.name())
                                                .build()))
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
