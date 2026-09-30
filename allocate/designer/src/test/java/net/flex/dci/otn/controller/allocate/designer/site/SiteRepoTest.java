package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.site.model.LinkOutput;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

class SiteRepoTest {

    @Test
    @SuppressWarnings("unchecked")
    void keepsUpdatedProtectedEndpointsInAtoZOrderWhenProtectionHasNoTransitNodes()
            throws Exception {
        Node aNode = node("Site-A#Ne-A");
        Node zNode = node("Site-D#Ne-D");
        CardTps aProtectionPeer = protectionPeer("Site-A#Ne-A#LINECARD-1-5#PORT-1-5-LINE_WEST");
        CardTps zProtectionPeer = protectionPeer("Site-D#Ne-D#LINECARD-1-5#PORT-1-5-LINE_WEST");

        SiteNodeInfo aSite = siteNode(aNode, aProtectionPeer, true);
        SiteNodeInfo zSite = siteNode(zNode, zProtectionPeer, false);

        NeInfo neInfo = mock(NeInfo.class);
        when(neInfo.getExternalLinkInfoFromTo("TILA", "TILA"))
                .thenReturn(Collections.emptyMap());

        LinkService linkService = mock(LinkService.class);
        when(linkService.createOtsLinks(anyString(), anyMap(), anyMap(), anyMap(), anySet()))
                .thenReturn(LinkOutput.builder()
                        .links(Collections.emptyList())
                        .internalLinks(Collections.emptyList())
                        .busyTpIds(new HashSet<>())
                        .build());

        NeNodeRepo neNodeRepo = mock(NeNodeRepo.class);
        when(neNodeRepo.updateInternalLink_And_BusyTp(any(Node.class), anyList(), anySet()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SiteRepo siteRepo = new SiteRepo();
        setField(siteRepo, "linkService", linkService);
        setField(siteRepo, "neNodeRepo", neNodeRepo);

        Method allocate = SiteRepo.class.getDeclaredMethod("allocate", SiteInput.class,
                NeInfo.class, List.class, SiteNodeInfo.class, SiteNodeInfo.class,
                java.util.Set.class, RoutingType.class);
        allocate.setAccessible(true);

        AllocatedSiteInfo result = (AllocatedSiteInfo) allocate.invoke(siteRepo,
                protectedFlex64Input(), neInfo, Collections.emptyList(), aSite, zSite,
                new HashSet<>(), RoutingType.Slave);

        List<String> endpointIds = result.getUpdatedProtectedNodes().stream()
                .map(updatedNode -> updatedNode.getNodeId().getValue())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("Site-A#Ne-A", "Site-D#Ne-D"), endpointIds);
    }

    private SiteInput protectedFlex64Input() {
        return SiteInput.builder()
                .nodesMap(Collections.emptyMap())
                .vendorName("COHERENT")
                .vendorType("CHASSIS2.0")
                .bandwidth(64)
                .isProtected(true)
                .grid(0)
                .plane("p")
                .planeId("p")
                .riskGroupName("r")
                .linkModel("6")
                .wdmBand(WDM_Band.C)
                .protectionType(ProtectionBidir1To1.class)
                .build();
    }

    private SiteNodeInfo siteNode(Node node, CardTps protectionPeer, boolean isAEnd) {
        return SiteNodeInfo.builder()
                .node(node)
                .links(Collections.emptyList())
                .xcs(Collections.emptyList())
                .slaveLinks(Collections.emptyList())
                .slaveXcs(Collections.emptyList())
                .slaveLeftPeer(isAEnd ? null : protectionPeer)
                .slaveRightPeer(isAEnd ? protectionPeer : null)
                .build();
    }

    private CardTps protectionPeer(String tpId) {
        Card card = new Card();
        card.setCardType("TILA");
        LinkedHashMap<String, String> ports = new LinkedHashMap<>();
        ports.put("LINE_WEST", tpId);
        return CardTps.builder()
                .card(card)
                .portNameTpMap(ports)
                .slavePortNameTpMap(ports)
                .thirdPortNameTpMap(Collections.emptyMap())
                .build();
    }

    private Node node(String nodeId) {
        return new NodeBuilder().setNodeId(new NodeId(nodeId)).build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
