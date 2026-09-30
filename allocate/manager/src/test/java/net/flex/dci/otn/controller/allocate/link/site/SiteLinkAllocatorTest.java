package net.flex.dci.otn.controller.allocate.link.site;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.LinkComputeResult;

class SiteLinkAllocatorTest {

    @Test
    void preservesThirdWithoutRequiringSlave() {
        LinkComputeResult result = SiteLinkAllocator.constructLinkComputeResult(
                RouteInfo.builder().main(emptyRoute()).third(emptyRoute()).build());

        assertNotNull(result.getMain());
        assertNull(result.getSlave());
        assertNotNull(result.getThird());
    }

    private Route emptyRoute() {
        return Route.builder()
                .nodes(Collections.emptyList())
                .links(Collections.emptyList())
                .xcs(Collections.emptyList())
                .build();
    }
}
