/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchOutput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchOutput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.AllocateOtStartInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtRouteInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.SegmentRouteInfo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TunnelRepo {

    @Autowired
    private OTAllocate otAllocate;

    @Autowired
    private TunnelSiteAllocate tunnelSiteAllocate;

    @Autowired
    private NEInfoConfig neInfoConfig;

    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private TunnelReuseOchAllocate tunnelReuseOchAllocate;
    @Autowired
    private TunnelNewOchAllocate tunnelNewOchAllocate;

    public RouteInfo allocate(TunnelInput input) throws NeDesignerException {

        validateInput(input);

        //output init
        List<Node> nodes = new ArrayList<>();
        List<Link> newLinks = new ArrayList<>();
        List<CrossConnections> newXcs = new ArrayList<>();
        //input
        NeInfo neInfo = neInfoConfig.getNeInfo(input.getVendorName(), input.getVendorType(), NodeType.TD.name());

        //point to point
        if (input.getSiteLinks().size() == 1) {
            Link siteLink = input.getSiteLinks().get(0);
            String siteLinkId = siteLink.getLinkId().getValue();
            String startSiteId = PhysicalNodeIdNamingRule.getSiteId(input.getSiteLinkSrcNode().getNodeId().getValue());

            //allocate OT Start->复用段
            AllocateOtStartInfo allocateOtStartInfo = otAllocate.allocateOtStart(input, startSiteId, siteLink, neInfo, input.getReusedNodesInDbSrc(), input.getReusedNodesInMemorySrc());
            OtRouteInfo otStartRouteInfo = allocateOtStartInfo.getOtRouteInfo();
            nodes.add(otStartRouteInfo.getNode());
            newXcs.add(otStartRouteInfo.getXc());
            final Link otStartOsLink = otStartRouteInfo.getLink();
            if (otStartOsLink != null) {
                newLinks.add(otStartOsLink);
                String olsTpStart = allocateOtStartInfo.getOlsPeerTps().getLeft();
                Node siteLinkSrcNode = input.getSiteLinkSrcNode();
                if (siteLinkSrcNode.getNodeId().getValue().equals(otStartRouteInfo.getNode().getNodeId().getValue())) {//好巧不巧，这个siteLink的node就是被重用了
                    nodes.remove(otStartRouteInfo.getNode());//后面通过ols修改以后再加。
                    siteLinkSrcNode = otStartRouteInfo.getNode();
                }
                SegmentRouteInfo startSegmentRouteInfo = tunnelSiteAllocate
                        .allocateSegment(olsTpStart, otStartOsLink, siteLink, allocateOtStartInfo.getFrequencyString(), allocateOtStartInfo.getCentFreq(), siteLinkSrcNode);
                newXcs.addAll(startSegmentRouteInfo.getXcs());
                nodes.add(startSegmentRouteInfo.getNode());
            }

            //allocate 复用段->OT End
            String endSiteId = PhysicalNodeIdNamingRule.getSiteId(input.getSiteLinkDestNode().getNodeId().getValue());

            //因为olsTp的port，statSite和endSite必须一样，所以allocateOtStart时就决定好了，endSite的port
            String olsTpEnd = allocateOtStartInfo.getOlsPeerTps() == null ? null : allocateOtStartInfo.getOlsPeerTps().getRight();
            OtRouteInfo otEndRouteInfo = otAllocate
                    .allocateOtEnd(input, endSiteId, neInfo, olsTpEnd, allocateOtStartInfo.getCentFreq(), input.getReusedNodesInDbDst(), input.getReusedNodesInMemoryDst(), siteLinkId);
            Link otEndOsLink = otEndRouteInfo.getLink();
            Node siteLinkDstNode = input.getSiteLinkDestNode();
            if (otEndOsLink != null) {
                newLinks.add(otEndOsLink);

                if (siteLinkDstNode.getNodeId().getValue().equals(otEndRouteInfo.getNode().getNodeId().getValue())) {//好巧不巧，这个siteLink的node就是被重用了
                    siteLinkDstNode = otEndRouteInfo.getNode();
                }
                SegmentRouteInfo endSegmentRouteInfo = tunnelSiteAllocate
                        .allocateSegment(olsTpEnd, otEndOsLink, siteLink, allocateOtStartInfo.getFrequencyString(), allocateOtStartInfo.getCentFreq(), siteLinkDstNode);
                newXcs.addAll(endSegmentRouteInfo.getXcs());
                nodes.add(endSegmentRouteInfo.getNode());
            }

            //add OT node, xc. OT node,xc必须要最后
            newXcs.add(otEndRouteInfo.getXc());
            //如果这个node是siteLinkNode，并且已经在SegmentRouteInfo那里被加过了，此处就不加
            if (!siteLinkDstNode.getNodeId().getValue().equals(otEndRouteInfo.getNode().getNodeId().getValue()) || otEndOsLink == null) {
                nodes.add(otEndRouteInfo.getNode());
            }

            //todo: slave暂时为空
            return RouteInfo.builder().main(Route.builder().links(newLinks).xcs(newXcs).nodes(nodes).build()).build();
        } else {
            //todo:ROADM
            return null;
        }


    }

    private void validateInput(TunnelInput input) throws NeDesignerException {
        if (input.getSiteLinks().isEmpty()) {
            throw new NeDesignerException("Invalid input, siteLinks can not be empty.");
        }
    }


    /**
     * Scenario: create tunnels on reused OCH link
     *
     * @param input
     * @return
     */
    public TunnelReuseOchOutput allocateReuseOch(TunnelReuseOchInput input) throws NeDesignerException {
        NeInfo neInfo = neInfoConfig.getNeInfo(input.getVendorName(), input.getVendorType(), NodeType.TD.name());
        Card otCardInfo = neInfo.getCardByCardType(input.getCardType());

        return tunnelReuseOchAllocate.allocate(input, otCardInfo);
    }

    public TunnelNewOchOutput allocateNewOch(TunnelNewOchInput input) throws NeDesignerException {
        return tunnelNewOchAllocate.allocate(input);
    }
}
