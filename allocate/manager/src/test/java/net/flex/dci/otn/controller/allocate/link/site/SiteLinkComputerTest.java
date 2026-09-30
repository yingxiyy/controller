/*
 * Copyright (c) 2019 Network Flex Any Comp. and others. All rights reserved.
 */

package net.flex.dci.otn.controller.allocate.link.site;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ComputeLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.SegmentBuilder;

class SiteLinkComputerTest {

    @Test
    void acceptsSegmentLevelRamanFlagWithoutCardVendorMarker() {
        Segment segment = new SegmentBuilder()
                .setSource("site-a")
                .setDestination("site-z")
                .setSourceCardVendor(Collections.singletonList("IRACL"))
                .setDestinationCardVendor(Collections.singletonList("IRACL"))
                .setRaman(true)
                .build();

        assertDoesNotThrow(() -> SiteLinkComputer.validateRamanPairs(Collections.singletonList(segment)));
    }

    @Test
    void stillRejectsSingleEndedLegacyCardVendorRaman() {
        Segment segment = new SegmentBuilder()
                .setSource("site-a")
                .setDestination("site-z")
                .setSourceCardVendor(Arrays.asList("IRACL", "RAMANCL_16"))
                .setDestinationCardVendor(Collections.singletonList("IRACL"))
                .build();

        assertThrows(NeDesignerException.class,
                () -> SiteLinkComputer.validateRamanPairs(Collections.singletonList(segment)));
    }

    @Test
    void computeOutputPreservesThirdWithoutRequiringSlave() {
        ComputeLinkOutput output = SiteLinkComputer.constructOutput(
                RouteInfo.builder().main(emptyRoute()).third(emptyRoute()).build());

        assertNotNull(output.getMain());
        assertNull(output.getSlave());
        assertNotNull(output.getThird());
    }

    private Route emptyRoute() {
        return Route.builder()
                .nodes(Collections.emptyList())
                .links(Collections.emptyList())
                .xcs(Collections.emptyList())
                .build();
    }
}
