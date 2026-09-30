package net.flex.dci.otn.controller.nms.nms.convertors.comparator;

import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;

/**
 * @version 1.0
 * @date 2022/11/7 15:51
 */
@Slf4j
public class PhyLinkComparator
        implements Comparator<Link> {

    @Override
    public int compare(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link1,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link2) {
        if (link1 == null
                || link1.getAugmentation(Link1.class).getPhysical().getLinkType()
                == null) {
            log.error("arg0 is null or {} linkType is null",
                    link1.getKey().getLinkId().getValue());
            return 0;
        }
        if (link2 == null
                || link2.getAugmentation(Link1.class).getPhysical().getLinkType()
                == null) {
            log.error("arg1 is null or {} linkType is null",
                    link2.getKey().getLinkId().getValue());
            return 1;
        }
        return link2.getAugmentation(Link1.class).getPhysical().getLinkType()
                .compareTo(link1.getAugmentation(Link1.class).getPhysical()
                        .getLinkType());
    }
}

