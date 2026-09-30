package net.flex.dci.otn.controller.nms.nms.convertors.connections;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.convertors.AbstractNmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/8/15
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
public class NmsViewLinkOutputConverters extends
        AbstractNmsOutputConverters<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link, Link> {

    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.VIEW_LINK;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> convert2NmsOutput(
            List<Link> linkList) {
        log.debug("convert to view link output");
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> links = new ArrayList<>();
        if (linkList == null || linkList.isEmpty()) {
            return links;
        }
        linkList.forEach(link -> {
            LinkBuilder linkBuilder = new LinkBuilder();
            linkBuilder.fieldsFrom(link);
            View view = link.getAugmentation(Link1.class).getView();
            ViewBuilder viewBuilder = new ViewBuilder(view);
            linkBuilder.setView(viewBuilder.build());
            links.add(linkBuilder.build());
        });
        return links;
    }
}
