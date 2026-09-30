package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.junit.jupiter.api.Test;

class ByteDance2Flex64ResourceLayoutTest {

    @Test
    void unprotectedLayoutKeepsSlotThreeSigCom2SelfLink() {
        assertKeepsSigCom2SelfLink(new ByteDance2Flex64ResourceLayout(false));
    }

    @Test
    void protectedOneToOneLayoutKeepsSlotThreeSigCom2SelfLink() {
        assertKeepsSigCom2SelfLink(new ByteDance2Flex64ResourceLayout(true));
    }

    @Test
    void protectedOneToTwoLayoutKeepsSlotThreeSigCom2SelfLink() {
        assertKeepsSigCom2SelfLink(new ByteDance2OneToTwoResourceLayout(64));
    }

    private void assertKeepsSigCom2SelfLink(SiteResourceLayout layout) {
        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("SIG", Arrays.asList(
                target("COM1"), target("COM2")));

        Map<String, List<ExternalLinkTo>> selected =
                layout.selectCardSelfLinks(relations, "FMUX_32");

        assertTrue(layout.hasCardSelfLinks("FMUX_32"));
        assertEquals(1, selected.get("SIG").size());
        assertEquals("COM2", selected.get("SIG").get(0).getPort());
    }

    private ExternalLinkTo target(String port) {
        ExternalLinkTo target = new ExternalLinkTo();
        target.setCardType("FMUX_32");
        target.setPort(port);
        return target;
    }
}
