/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class TunnelLinkService {

    @Autowired
    private LinkRepo linkRepo;

    public Link createOsLink(PickedOtResource tpOtNode, String olsTp, Boolean isReversed) throws NeDesignerException {
        String source, destination;
        if (isReversed) {
            source = olsTp;
            destination = tpOtNode.getPickedOtTps().getLtp();
        } else {
            source = tpOtNode.getPickedOtTps().getLtp();
            destination = olsTp;
        }

        Link link = linkRepo.createLink(source, destination, LinkType.OsLink);

        if (isLinkExisted(tpOtNode.getNode(), link.getLinkId().getValue())) {
            log.debug("No need to create new link, because link exists already. {}", link.getLinkId().getValue());
            return null;
        }

        return link;
    }

    public Link createOsLink(String lPortTp, String olsTp, Boolean isReversed) throws NeDesignerException {
        String source, destination;
        if (isReversed) {
            source = olsTp;
            destination = lPortTp;
        } else {
            source = lPortTp;
            destination = olsTp;
        }

        return linkRepo.createLink(source, destination, LinkType.OsLink);

    }

    private boolean isLinkExisted(Node node, String linkId) {
        return node.getAugmentation(Node1.class).getPhysical().getInternalLinks().stream().filter(internalLinks -> internalLinks.getLinkRef().equals(linkId)).findFirst().isPresent();

    }

    public Link createRegLink(String srcTpId, String destTpId) throws NeDesignerException {
        return linkRepo.createLink(srcTpId, destTpId, LinkType.OsLink, Collections.EMPTY_LIST, LinkDirection.Unidirection);
    }
}
