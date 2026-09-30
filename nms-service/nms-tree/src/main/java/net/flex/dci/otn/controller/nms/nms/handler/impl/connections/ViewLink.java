/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @author: xinyzhao
 * @date: 2021/4/8
 */
@Slf4j
public class ViewLink extends AbstractTopoLink {

    public ViewLink(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("get site link based on view link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        List<Link> output = new LinkedList<>();
        TopologyId siteTopoId = new TopologyId(SITE_TOPO_KEY);
        for (SupportingLink sl : ntLink.getSupportingLink()) {
            output.add(netconfTopology.getLink(siteTopoId, sl.getLinkRef()));
        }
        return output;
    }
}
