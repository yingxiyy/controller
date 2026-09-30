package net.flex.dci.otn.controller.nms.nms.component.viewTopo.impl;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class EmptyViewTopo extends AbstractViewTopo {


    @Override
    public ViewLinkType supportViewLinkType() {
        return null;
    }

    @Override
    public GetViewLinkByPlaneStartwithOutput getTopo(List<Link> siteLinks, String plane) {
        return defaultReturn();
    }

    @Override
    protected ViewTopoDto getViewTopoDto(List<Link> siteLinks) {
        return null;
    }


}
