/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery1.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.discovery.discovery1.utils.AsynchronousExecutor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverDiscardInput;
import org.springframework.stereotype.Component;


//
// for each tunnel, send tti  on A side, and then Z side TTI recieved should be sent value.
//
@Component
@Slf4j
public class DiscoveryDiscard {

    public void discoveryDiscard(RecoverDiscardInput input) {
        List<String> tunnelIds = input.getResourceId();
        log.debug("discovery discard tunnel ,the tunnel ids is:{}", tunnelIds);
        tunnelIds.forEach(tunnelId -> {
            AsynchronousExecutor.execute(() -> {
                DiscoveryDiscardImpl.getInstance().doIt(tunnelId);
            });
        });
    }

//    private List<String> tunnelIdList;
//
//    public DiscoveryDiscard(List<String> resourceList) {
//        this.tunnelIdList = resourceList;
//    }

//    public void start() {
//        for (String tunnelId : tunnelIdList) {
//            DiscoveryDiscardImpl.getInstance().doIt(tunnelId);
//        }
//    }
}
