package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.Collections;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.mdoel.OchProtectionLegPatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;

class ChangedObjectProtectionLegPatchTest {

    @Test
    void protectionLegPatchIsTrackedSeparatelyFromOchLinkRewrite() {
        ChangedObject changedObject = new ChangedObject();
        Link updatedOchLink = new LinkBuilder()
                .setLinkId(new LinkId("och-1"))
                .build();
        ExplictRoute explictRoute = new ExplictRouteBuilder().build();
        SupportingLink addedSupportingLink = new SupportingLinkBuilder()
                .setLinkRef(new LinkId("site-link-1"))
                .setKey(new SupportingLinkKey(new LinkId("site-link-1")))
                .build();

        OchProtectionLegPatch patch = OchProtectionLegPatch.addOrUpdate(
                updatedOchLink, explictRoute, Collections.singletonList(addedSupportingLink),
                Collections.singletonList("removed-site-link-1"));
        changedObject.addChangedOchProtectionLegPatch(patch);

        Assertions.assertSame(patch,
                changedObject.getChangedOchProtectionLegPatchList().get("och-1"));
        Assertions.assertSame(explictRoute, patch.getExplictRoute());
        Assertions.assertTrue(changedObject.getChangedOchLinkList().isEmpty());
    }
}
