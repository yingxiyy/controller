package net.flex.dci.otn.controller.implement.tunnel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import net.flex.dci.otc.common.util.RouteInfo;
import org.junit.jupiter.api.Test;

class TunnelElectricFollowerScopeTest {

    private static final String NODE_ID = "Site-1#Ne-1";
    private static final String PRIVATE_TP = NODE_ID + "#LINECARD-1-1#PORT-1-1-C1";
    private static final String SHARED_TP = NODE_ID + "#LINECARD-1-1#PORT-1-1-L1";

    @Test
    void removeFromKeepsNodeUsedBySharedOchResource() {
        RouteInfo privateRoute = routeWithTps(PRIVATE_TP);
        RouteInfo commonRoute = routeWithTps(PRIVATE_TP, SHARED_TP);

        new TunnelElectricFollowerScope(privateRoute).removeFrom(commonRoute);

        assertEquals(Collections.singletonList(SHARED_TP), commonRoute.getTpIdList());
        assertEquals(Collections.singletonList(NODE_ID), commonRoute.getNodeIdList());
    }

    @Test
    void removeFromDropsNodeOwnedOnlyByPrivateTunnelResource() {
        RouteInfo privateRoute = routeWithTps(PRIVATE_TP);
        RouteInfo commonRoute = routeWithTps(PRIVATE_TP);

        new TunnelElectricFollowerScope(privateRoute).removeFrom(commonRoute);

        assertFalse(commonRoute.getNodeIdList().contains(NODE_ID));
    }

    private RouteInfo routeWithTps(String... tpIds) {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setTpIdList(new java.util.ArrayList<>(Arrays.asList(tpIds)));
        routeInfo.setNodeIdList(new java.util.ArrayList<>(Collections.singletonList(NODE_ID)));
        return routeInfo;
    }
}
