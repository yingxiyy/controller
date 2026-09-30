package net.flex.dci.otn.controller.allocate.designer.reallocate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReInternalLinkRepo {

    @Autowired
    private LinkRepo linkRepo;

    public List<InternalLinks> reallocate(String nodeId, List<InternalLinks> oldInternalLinks, HashMap<String, Link> newLinks) {
        List<InternalLinks> newInternalLinks = new ArrayList<>();
        for (InternalLinks oldInternalLink : oldInternalLinks) {
            String oldLinkId = oldInternalLink.getLinkRef();
            Link newLink = newLinks.get(oldLinkId);
            if (newLink == null) {
                newInternalLinks.add(oldInternalLink);
                continue;
            }

            if (newLink.getLinkId().getValue().contains(nodeId)) {
                InternalLinks newInternalLink = linkRepo.createInternalLink(nodeId, newLink);
                newInternalLinks.add(newInternalLink);
            }

        }
        return newInternalLinks;
    }

    public List<InternalLinks> getInternalLinksByNode(String nodeId, List<Link> newLinksList) {
        return newLinksList.stream().filter(item->item.getLinkId().getValue().contains(nodeId)).map(item->linkRepo.createInternalLink(nodeId,item)).collect(Collectors.toList());
    }
}
