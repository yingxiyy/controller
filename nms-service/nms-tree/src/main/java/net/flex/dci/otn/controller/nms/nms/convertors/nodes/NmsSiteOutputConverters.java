package net.flex.dci.otn.controller.nms.nms.convertors.nodes;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 10:34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NmsSiteOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node, Node> {


    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> convert2NmsOutput(
            List<Node> elements) {
        log.info("convert site node to nms output element");
        if (elements.isEmpty()) {
            return new ArrayList<>();
        }
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> output = elements.stream()
                .map(
                        siteNode -> {
                            NodeBuilder nmsSiteNodeBuilder = new NodeBuilder();
                            nmsSiteNodeBuilder.fieldsFrom(siteNode);
                            nmsSiteNodeBuilder.setSupportingNode(null);
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder sb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(
                                    siteNode.getAugmentation(
                                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                                            .getSite());
                            sb.setSupportingRack(null);
                            if (siteNode.getTerminationPoint() != null) {
                                List<TerminationPoint> terminationPoints = siteNode.getTerminationPoint()
                                        .stream().map(terminationPoint -> {
                                            TerminationPointBuilder tb = new TerminationPointBuilder(
                                                    terminationPoint);
                                            tb.fieldsFrom(terminationPoint.getAugmentation(
                                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1.class));
                                            return tb.build();
                                        }).collect(Collectors.toList());
                                nmsSiteNodeBuilder.setTerminationPoint(terminationPoints);
                            }
                            nmsSiteNodeBuilder.setSite(sb.build());
                            nmsSiteNodeBuilder.setTopologyRef(new TopologyId(SITE_TOPO_KEY));
                            nmsSiteNodeBuilder.setNodeId(siteNode.getNodeId());
                            return nmsSiteNodeBuilder.build();
                        }
                ).collect(
                        Collectors.toList());

        return output;
    }


    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.SITE_NODE;
    }
}
