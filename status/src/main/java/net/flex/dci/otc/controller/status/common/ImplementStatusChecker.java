/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ImplementStatusChecker {

    @Autowired
    private PhyLinkDao phyLinkDao;

    @Autowired
    private SiteLinkDao siteLinkDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private TunnelDao tunnelDao;

//    public boolean shouldUpdate(StatusMessage message) {
//        String id = message.getId();
//        if (message.getYangObjectType() == YangObjectType.PhyLink) {
////            InstanceIdentifier<Physical> iid = TopoNameConstants.PHY_TOPO_IID
////                    .child(Link.class, new LinkKey(new LinkId(id)))
////                    .augmentation(
////                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
////                    .child(Physical.class);
////            Physical phy = (Physical) mongoDao.readData(iid, DataStoreType.OPERATIONAL);
//            Physical phy = phyLinkDao.getLinkPhysical(id);
//            if (phy != null
//                    && phy.getImplementState() == ImplementState.Implement) {
//                return true;
//            }
//        } else if (message.getYangObjectType() == YangObjectType.SiteLink) {
//
//            Site site = siteLinkDao.getSiteLinkAttributeSite(id);
//            if (site != null
//                    && site.getImplementState() == ImplementState.Implement) {
//                return true;
//            }
//        } else if (message.getYangObjectType() == YangObjectType.OchLink) {
////            InstanceIdentifier<Och> iid = TopoNameConstants.SITE_TOPO_IID
////                    .child(Link.class, new LinkKey(new LinkId(id)))
////                    .augmentation(
////                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
////                    .child(
////                            Och.class);
//            Och och = ochLinkDao.getLinkAttributeOch(id);
//            if (och != null
//                    && och.getImplementState() == ImplementState.Implement) {
//                return true;
//            }
//        } else if (message.getYangObjectType() == YangObjectType.Tunnel) {
////            InstanceIdentifier<Tunnel> iid = TopoNameConstants.SITE_TOPO_IID
////                    .augmentation(Topology1.class).child(Tunnel.class,
////                            new TunnelKey(new Uri(id)));
//            Tunnel tunnel = tunnelDao.getTunnelById(id);
//            if (tunnel != null
//                    && tunnel.getImplementState() == ImplementState.Implement) {
//                return true;
//            }
//        } else if (message.getYangObjectType() == YangObjectType.ViewLink) {
//            return true;
//        } else if (message.getYangObjectType() == YangObjectType.ViewNode) {
//            return true;
//        } else if (message.getYangObjectType() == YangObjectType.SiteNode) {
//            return true;
//        }
//        return false;
//    }
}
