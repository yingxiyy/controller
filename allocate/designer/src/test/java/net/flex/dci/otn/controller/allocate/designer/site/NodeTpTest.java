package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.junit.jupiter.api.Test;

class NodeTpTest {

    @Test
    void oneToTwoSlavePeerUsesLayoutOlpPortRegardlessOfCardOrder() {
        CardTps fmux = cardTps("FMUX_32", map("SIGB", "fmux-sigb"),
                Collections.emptyMap());
        CardTps olp = cardTps("OLP3_3", map("1B", "olp-1b"),
                map("1C", "olp-1c"));

        NodeTp sourceEnd = nodeTp(Arrays.asList(fmux, olp));
        NodeTp reversedDestinationEnd = nodeTp(Arrays.asList(olp, fmux));

        assertSame(olp, sourceEnd.getSlaveRightPeer());
        assertSame(olp, sourceEnd.getSlaveLeftPeer());
        assertSame(olp, reversedDestinationEnd.getSlaveRightPeer());
        assertSame(olp, reversedDestinationEnd.getSlaveLeftPeer());
        assertSame(olp, sourceEnd.getThirdRightPeer());
        assertSame(olp, reversedDestinationEnd.getThirdLeftPeer());
    }

    @Test
    void flexOneToOneWithoutOlpPortKeepsFmuxSlavePeer() {
        CardTps fmux = cardTps("FMUX_32", map("SIGB", "fmux-sigb"),
                Collections.emptyMap());
        CardTps olp = cardTps("OLP3_3", map("1B", "olp-1b"),
                Collections.emptyMap());

        NodeTp node = nodeTp(Arrays.asList(fmux, olp),
                new ByteDance2FlexOneToOneResourceLayout());

        assertSame(fmux, node.getSlaveRightPeer());
        assertSame(fmux, node.getSlaveLeftPeer());
    }

    @Test
    void oneToTwoDoesNotFallBackToFmuxWhenRequiredOlpPortIsMissing() {
        CardTps fmux = cardTps("FMUX_32", map("SIGB", "fmux-sigb"),
                Collections.emptyMap());

        NodeTp node = nodeTp(Collections.singletonList(fmux));

        assertNull(node.getSlaveRightPeer());
        assertNull(node.getSlaveLeftPeer());
    }

    private NodeTp nodeTp(List<CardTps> cardTps) {
        return nodeTp(cardTps, new ByteDance2OneToTwoResourceLayout(32));
    }

    private NodeTp nodeTp(List<CardTps> cardTps, SiteResourceLayout resourceLayout) {
        return NodeTp.builder()
                .nodeId("node")
                .nodeTpList(Collections.emptyList())
                .equipments(Collections.emptyList())
                .cardTps(cardTps)
                .slaveCardTps(Collections.emptyList())
                .thirdCardTps(Collections.emptyList())
                .resourceLayout(resourceLayout)
                .build();
    }

    private CardTps cardTps(String cardType, Map<String, String> slavePorts,
            Map<String, String> thirdPorts) {
        Card card = new Card();
        card.setCardType(cardType);
        return CardTps.builder()
                .card(card)
                .portNameTpMap(Collections.emptyMap())
                .slavePortNameTpMap(slavePorts)
                .thirdPortNameTpMap(thirdPorts)
                .busyTpIds(Collections.emptySet())
                .build();
    }

    private Map<String, String> map(String portName, String tpId) {
        Map<String, String> result = new HashMap<>();
        result.put(portName, tpId);
        return result;
    }
}
