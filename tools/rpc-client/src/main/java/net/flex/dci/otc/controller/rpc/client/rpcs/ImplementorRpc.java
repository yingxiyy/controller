/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateSitelinkSyncInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateSitelinkSyncOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncOutput;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/9 11:18
 */
public interface ImplementorRpc {

    /**
     * site-topology:update-sitelink-sync
     *
     * @param input
     * @return
     */
    UpdateSitelinkSyncOutput updateSiteLink(UpdateSitelinkSyncInput input) throws CommonException;

    /**
     * site-topology:update-link
     *
     * @param input
     * @return
     */
    UpdateLinkOutput updateLink(UpdateLinkInput input) throws CommonException;

    /**
     * tunnel:update-tunnel
     *
     * @param input
     * @return
     */
    UpdateTunnelOutput updateTunnel(UpdateTunnelInput input) throws CommonException;


    /**
     * site-topology:update-sitelink-sync
     *
     * @param input
     * @return
     */
    UpdateTunnelSyncOutput updateTunnelSync(UpdateTunnelSyncInput input) throws CommonException;
}
