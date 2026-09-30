package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 1/21/2024 5:00 PM
 */
@Component
@Slf4j
public class DefaultViewLink extends AbstractViewLink {

    @Override
    public NMSViewLinkType supportViewLinkType() {
        return NMSViewLinkType.ALL;
    }

    @Override
    public List<Topology> getRefViewLinks() {

        return null;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getRefViewLinkInfoByMultiplexLink(
            String plane, List<Link> siteLinks,
            List<Link> ochLinks) {
        return null;
    }
}
