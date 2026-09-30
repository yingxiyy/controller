package net.flex.dci.otn.controller.nms.nms.component.connection;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * 2026/5/26
 *
 * @author musa
 * @version 1.0
 **/
public interface SiteLinkRelationCalculator {

    List<RelatedSiteLink> getSiteLinkRelation(List<Link> siteLinks);
}
