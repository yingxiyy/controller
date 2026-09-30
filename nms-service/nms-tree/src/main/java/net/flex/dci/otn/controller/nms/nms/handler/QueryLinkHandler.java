/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.utils.NetConfConvertors;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/8
 */

@Component
@Slf4j
public class QueryLinkHandler extends AbstractBaseHandler {

    public QueryLinkHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    public GetSiteLinkByNodeIpOutputBuilder getSiteLinkByNodeIp(Node node) throws Exception {
        List<Node> output =
                new ArrayList<Node>();
        output.add(NetConfConvertors.convertPhyNode(node));

        GetSiteLinkByNodeIpOutputBuilder builder = new GetSiteLinkByNodeIpOutputBuilder();

        Topology topo = netconfTopology
                .getTopology(new TopologyId(SiteTopology.QNAME.getLocalName()));
        for (Link operLink : topo.getLink()) {
            String desTp = operLink.getDestination().getDestTp().getValue();
            String desTmp[] = desTp.split("#");
            String desNeId = desTmp[0] + "#" + desTmp[1];
            String srcTp = operLink.getSource().getSourceTp().getValue();
            String srcTmp[] = srcTp.split("#");
            String srcNeId = srcTmp[0] + "#" + srcTmp[1];

            //check op
            boolean opsModel = false;
            if (operLink.getAugmentation(Link1.class) != null
                    && operLink.getAugmentation(Link1.class).getSite() != null
                    && operLink.getAugmentation(Link1.class).getSite().getProperties() != null
                    && operLink.getAugmentation(Link1.class).getSite().getProperties().getProperty()
                    != null) {
                for (Property property : operLink.getAugmentation(Link1.class).getSite()
                        .getProperties().getProperty()) {
                    if ("model".equals(property.getName())) {
                        if ("3".equals(property.getValue()) || "4".equals(property.getValue())) {
                            opsModel = true;
                        }
                    }
                }
            }

            if (opsModel) {
                if (node.getNodeId().getValue().equals(desNeId)) {
                    Link siteLink = NetConfConvertors.convertLink(operLink, netconfTopology);
                    builder.fieldsFrom(siteLink);
                    builder.setSite(siteLink.getAugmentation(Link1.class).getSite());
                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> srcNode
                            = new NodeHandler(netconfTopology)
                            .getPhyNode(new TopologyId(TopoNameConstants.Phy_Topo_Key),
                                    new NodeId(srcNeId), null, null, null,
                                    null, null);

                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> srcSite
                            = new NodeHandler(netconfTopology)
                            .getSiteNode(new TopologyId(TopoNameConstants.Site_Topo_Key),
                                    operLink.getSource().getSourceNode(), null, null, null, null,
                                    null);

                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> desSite
                            = new NodeHandler(netconfTopology)
                            .getSiteNode(new TopologyId(TopoNameConstants.Site_Topo_Key),
                                    operLink.getDestination().getDestNode(), null, null, null, null,
                                    null);

                    output.add(NetConfConvertors.convertPhyNode(srcNode.get(0)));
                    output.add(NetConfConvertors.convertSiteNode(srcSite.get(0)));
                    output.add(NetConfConvertors.convertSiteNode(desSite.get(0)));
                } else if (node.getNodeId().getValue().equals(srcNeId)) {
                    Link siteLink = NetConfConvertors.convertLink(operLink, netconfTopology);
                    builder.fieldsFrom(siteLink);
                    builder.setSite(siteLink.getAugmentation(Link1.class).getSite());

                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> desNode
                            = new NodeHandler(netconfTopology)
                            .getPhyNode(new TopologyId(TopoNameConstants.Phy_Topo_Key),
                                    new NodeId(desNeId), null, null, null,
                                    null, null);

                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> srcSite
                            = new NodeHandler(netconfTopology)
                            .getSiteNode(new TopologyId(TopoNameConstants.Site_Topo_Key),
                                    operLink.getSource().getSourceNode(), null, null, null, null,
                                    null);

                    List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> desSite
                            = new NodeHandler(netconfTopology)
                            .getSiteNode(new TopologyId(TopoNameConstants.Site_Topo_Key),
                                    operLink.getDestination().getDestNode(), null, null, null, null,
                                    null);

                    output.add(NetConfConvertors.convertPhyNode(desNode.get(0)));
                    output.add(NetConfConvertors.convertSiteNode(srcSite.get(0)));
                    output.add(NetConfConvertors.convertSiteNode(desSite.get(0)));
                }
            }
        }
        builder.setNode(output);
        return builder;
    }


}
