package net.flex.dci.otn.controller.allocate.link.site;

import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface CreatorMethod_I<T> {

  Source getLinkSrcTermination(RouteInfo routeResource);
  Destination getLinkDstTermination(RouteInfo routeResource);

  RouteInfo allocateResource(NeDesigner neDesigner, T input, CreateSiteLinkParam param) throws NeDesignerException;

  SiteNodeCorrelateResource createSiteNodeCorrelateResource(Node siteNode, Link siteLink, Node phyNode);
}
