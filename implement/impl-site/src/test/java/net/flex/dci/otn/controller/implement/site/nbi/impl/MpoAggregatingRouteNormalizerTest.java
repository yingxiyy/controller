package net.flex.dci.otn.controller.implement.site.nbi.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.NeYangModel;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;

class MpoAggregatingRouteNormalizerTest {

    private static final String NODE = "Site-A#Ne-A";
    private static final String FMUX_PREFIX = NODE + "#LINECARD-1-7#PORT-1-7-MPO";
    private static final String MUX_PREFIX = NODE + "#MUX-1-50#PORT-1-50-MPO";

    @Test
    void enablesMpoNormalizationForLegacyAndBone20ByteDanceModelsOnly() {
        assertTrue(MpoAggregatingRouteNormalizer.supports(NeYangModel.ByteDance));
        assertTrue(MpoAggregatingRouteNormalizer.supports(NeYangModel.Chassis20));
        assertFalse(MpoAggregatingRouteNormalizer.supports(NeYangModel.Tencent));
    }

    @Test
    void expandsFlex64ExtensionFmuxFromActualSupportingLinks() {
        Link siteLink = siteLinkWithExtensionFmuxLinks();
        MpoAggregatingRouteNormalizer normalizer =
                new MpoAggregatingRouteNormalizer(null, siteLink);

        assertEquals(Arrays.asList(
                FMUX_PREFIX + "1", FMUX_PREFIX + "2", FMUX_PREFIX + "3", FMUX_PREFIX + "4"),
                normalizer.expandTp(FMUX_PREFIX));
        assertEquals(extensionFmuxLinkIds(), normalizer.expandLink(
                "OmsLink-" + FMUX_PREFIX + "-" + MUX_PREFIX));
    }

    private Link siteLinkWithExtensionFmuxLinks() {
        List<SupportingLink> links = extensionFmuxLinkIds().stream()
                .map(linkId -> new SupportingLinkBuilder()
                        .setLinkRef(new LinkId(linkId))
                        .build())
                .collect(Collectors.toList());
        return new LinkBuilder().setSupportingLink(links).build();
    }

    private List<String> extensionFmuxLinkIds() {
        List<String> links = new ArrayList<>();
        for (int index = 1; index <= 4; index++) {
            links.add("OmsLink-" + FMUX_PREFIX + index + "-" + MUX_PREFIX + (index + 4));
        }
        return links;
    }
}
