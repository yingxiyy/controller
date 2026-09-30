package net.flex.dci.otn.controller.implement.site.nbi.impl.ase;

import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;

import java.util.Collection;

public class AseInitialize {
    private ChangedObject changedObject;

    public AseInitialize(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    public Collection<String> getXcList(String ochLinkId) {
        RouteInfo rInfo = new RouteInfo();

        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
        return rInfo.getXcIdList();
    }

//    public void implement(String ochLinkId) {
//        RouteInfo rInfo = new RouteInfo();
//
//        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
//        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
//        rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
//        return rInfo.getXcIdList();
//    }
}
