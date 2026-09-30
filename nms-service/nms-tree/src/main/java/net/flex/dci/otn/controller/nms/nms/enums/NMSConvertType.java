package net.flex.dci.otn.controller.nms.nms.enums;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @version 1.0
 * @date 2022/11/6 15:04
 */
public enum NMSConvertType {

    DEFAULT,
    PHY_LINK,
    TUNNEL,
    SITE_LINK,
    PHY_NODE,
    SITE_NODE,
    TERMINATION_POINT,
    OCH_LINK,
    VIEW_LINK,
    VIEW_SITE;

    public static NMSConvertType getConvertType(DataObject dataObject) {
        NMSConvertType nmsConvertType = DEFAULT;
        if (dataObject instanceof Node) {
            Node node = ((Node) dataObject);
            Node1 phyNode = node.getAugmentation(Node1.class);
            if (null != phyNode) {
                nmsConvertType = PHY_NODE;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteNode = node.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
            if (null != siteNode) {
                nmsConvertType = SITE_NODE;
            }
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1 viewNode = node.getAugmentation(
                    org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1.class);
            if (null != viewNode) {
                nmsConvertType = VIEW_SITE;
            }
        } else if (dataObject instanceof Link) {
            Link link = (Link) dataObject;
            Link1 siteLink = link.getAugmentation(Link1.class);
            if (siteLink != null) {
                nmsConvertType = SITE_LINK;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink = link.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            if (phyLink != null) {
                nmsConvertType = PHY_LINK;
            }
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1 viewLink = link.getAugmentation(
                    org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class);
            if (null != viewLink) {
                nmsConvertType = VIEW_LINK;
            }
        } else if (dataObject instanceof Tunnel) {

            nmsConvertType = TUNNEL;
        } else if (dataObject instanceof TerminationPoint) {
            nmsConvertType = TERMINATION_POINT;
        }
        return nmsConvertType;
    }

}
