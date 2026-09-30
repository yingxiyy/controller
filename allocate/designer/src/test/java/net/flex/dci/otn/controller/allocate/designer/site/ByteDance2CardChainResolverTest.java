package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import org.junit.jupiter.api.Test;

class ByteDance2CardChainResolverTest {

    private final List<String> legacyFlexOtmChain =
            Arrays.asList("PANEL", "MUXPANEL", "CMUX", "OA");
    private final List<String> legacyFixedOtmChain =
            Arrays.asList("PANEL", "MUX64", "OA");
    private final List<String> legacyProtectedFixedOtmChain =
            Arrays.asList("PANEL", "MUX64", "OP", "OA");

    @Test
    void legacyProductTypeKeepsOriginalCardChain() {
        assertEquals(legacyFlexOtmChain, ByteDance2CardChainResolver.resolve(
                "COHERENT", "CHASSIS", WDM_Band.C, 0, 32, legacyFlexOtmChain));
    }

    @Test
    void bandwidth32UsesOneFmuxAndTila() {
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "ILA"),
                ByteDance2CardChainResolver.resolve(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 0, 32, legacyFlexOtmChain));
    }

    @Test
    void bandwidth64UsesTwoFmuxCardsAndTila() {
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "ILA"),
                ByteDance2CardChainResolver.resolve(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 0, 64, legacyFlexOtmChain));
    }

    @Test
    void missingBandwidthKeepsByteDance20BackwardCompatibleAt64Channels() {
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "ILA"),
                ByteDance2CardChainResolver.resolve(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 0, null, legacyFlexOtmChain));
    }

    @Test
    void fixed75ReusesSiteModelChainAndMapsLegacyCards() {
        assertEquals(Arrays.asList("PANEL", "MUX", "ILA"),
                ByteDance2CardChainResolver.resolveFixedOtm(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 75, false, "T", legacyFixedOtmChain));
    }

    @Test
    void fixed150ReusesSiteModelChainAndMapsLegacyCards() {
        assertEquals(Arrays.asList("PANEL", "MUX", "ILA"),
                ByteDance2CardChainResolver.resolveFixedOtm(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 150, false, "T", legacyFixedOtmChain));
    }

    @Test
    void protectedFixedGridAddsOlpBetweenMuxAndTila() {
        assertEquals(Arrays.asList("PANEL", "MUX", "OP", "ILA"),
                ByteDance2CardChainResolver.resolveFixedOtm(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 75, true, "T",
                        legacyProtectedFixedOtmChain));
        assertEquals(Arrays.asList("PANEL", "MUX", "OP", "ILA"),
                ByteDance2CardChainResolver.resolveFixedOtm(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 150, true, "T",
                        legacyProtectedFixedOtmChain));
    }

    @Test
    void roadm64AddDropUsesTwoFmuxCards() {
        List<String> roadmAddDropChain =
                Arrays.asList("PANEL", "MUXPANEL", "CMUX", "PANEL", "IRA");

        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "PANEL", "IRA"),
                ByteDance2CardChainResolver.resolve(
                        "COHERENT", "CHASSIS2.0", WDM_Band.C, 0, 64, roadmAddDropChain));
        assertEquals(roadmAddDropChain, ByteDance2CardChainResolver.resolve(
                "COHERENT", "CHASSIS2.0", WDM_Band.C, 0, 32, roadmAddDropChain));
    }

    @Test
    void legacyFixedGridDoesNotOverrideOriginalSiteModel() {
        assertEquals(legacyFixedOtmChain, ByteDance2CardChainResolver.resolveFixedOtm(
                "COHERENT", "CHASSIS", WDM_Band.C, 75, false, "T", legacyFixedOtmChain));
    }

    @Test
    void omspUsesTilaAsTheSlaveLineCard() {
        assertEquals(Arrays.asList("ILA"), ByteDance2CardChainResolver.resolveProtectedPeers(
                "COHERENT", "CHASSIS2.0", Arrays.asList("OA")));
    }

    @Test
    void legacyOmspKeepsTheOriginalSlaveLineCard() {
        assertEquals(Arrays.asList("OA"), ByteDance2CardChainResolver.resolveProtectedPeers(
                "COHERENT", "CHASSIS", Arrays.asList("OA")));
    }

    @Test
    void fmuxMainPortDependsOnProtectionMode() {
        assertEquals("SIG", ByteDance2CardChainResolver.getFmuxMainLinePort(false));
        assertEquals("SIGA", ByteDance2CardChainResolver.getFmuxMainLinePort(true));
    }
}
