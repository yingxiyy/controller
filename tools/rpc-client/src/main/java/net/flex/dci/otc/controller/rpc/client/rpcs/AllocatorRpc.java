/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelOutput;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/9 11:17
 */
public interface AllocatorRpc {

    /**
     * site-topology:create-link
     *
     * @param input
     * @return
     * @throws CommonException
     */
    CreateLinkOutput createSiteLink(CreateLinkInput input) throws CommonException;


    /**
     * site-topology:remove-link
     *
     * @param input
     * @return
     * @throws CommonException
     */
    RemoveLinkOutput removeSiteLink(RemoveLinkInput input) throws CommonException;


    /**
     * tunnel:create-tunnel
     *
     * @param input
     * @return
     * @throws CommonException
     */
    CreateTunnelOutput createTunnel(CreateTunnelInput input) throws CommonException;


    /**
     * tunnel:remove-tunnel
     *
     * @param input
     * @return
     * @throws CommonException
     */
    RemoveTunnelOutput removeTunnel(RemoveTunnelInput input) throws CommonException;

}
