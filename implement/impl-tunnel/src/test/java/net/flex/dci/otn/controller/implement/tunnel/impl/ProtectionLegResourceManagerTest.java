package net.flex.dci.otn.controller.implement.tunnel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.PropertyTool;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;

class ProtectionLegResourceManagerTest {

    @Test
    void rejectRemoveLegWhileBindingThirdLegIsUnfinished() {
        Properties properties = PropertyTool.addProperty(null, "binding3rdLeg", "true");
        Och och = new OchBuilder()
                .setImplementState(ImplementState.PartialImplement)
                .setProperties(properties)
                .build();

        CommonException thrown = assertThrows(CommonException.class,
                () -> ProtectionLegResourceManager.validateNoBindingThirdLeg(och));

        assertTrue(thrown.getMessage().contains("binding third leg is unfinished"));
    }

    @Test
    void restoreTunnelReadsMarkerBeforeMutablePropertyCleanup() {
        Properties properties = PropertyTool.addProperty(null,
                ProtectionLegResourceManager.REMOVE_LEG_RESTORE_TUNNEL, "true");
        properties = PropertyTool.addProperty(properties,
                ProtectionLegResourceManager.PROTECTION_CHANGE_ACTION,
                ProtectionLegResourceManager.PROTECTION_CHANGE_ACTION_REMOVE_LEG);
        Tunnel tunnel = new TunnelBuilder()
                .setTunnelId(new Uri("Tunnel-test"))
                .setImplementState(ImplementState.Doimplementing)
                .setProperties(properties)
                .build();

        Tunnel restored = ProtectionLegResourceManager.restoreTunnelAfterRemoveLeg(tunnel, null, 2);

        assertEquals(ImplementState.Implement, restored.getImplementState());
        assertFalse(PropertyTool.existProperty(restored.getProperties(),
                ProtectionLegResourceManager.REMOVE_LEG_RESTORE_TUNNEL));
        assertFalse(PropertyTool.existProperty(restored.getProperties(),
                ProtectionLegResourceManager.PROTECTION_CHANGE_ACTION));
        assertTrue(PropertyTool.existProperty(restored.getProperties(), "leg-required"));
    }
}
