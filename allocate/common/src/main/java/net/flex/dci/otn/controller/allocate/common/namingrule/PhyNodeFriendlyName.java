/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;

import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static net.flex.dci.otc.common.util.Constant.PropKey_HostName;

/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
@Component
public class PhyNodeFriendlyName {

    //site名称-厂家名称-site中的网元序号

    private String buildFriendlyName(String siteName, String vendorName, String sequence) {
        String friendlyName = siteName.trim() + "-" + vendorName.trim() + "-" + sequence;
        return friendlyName;
    }

    private String createFriendlyName(Node siteNode, Node phyNode) {
        log.debug("start create phy node friendlyName");
        String siteCode = "";
        String vendorName = "";
        String suffix = "";
        try {
            String location = "unset";
            String rackSequence = "0";
            Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();
            String siteFriendlyName = siteAttr.getFriendlyName();
            siteCode = siteAttr.getCode();

            boolean found = false;
            vendorName = phyNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical().getVendorName();
            for (SupportingRack rack : siteAttr.getSupportingRack()) {
                for (SupportingNe sNe : rack.getSupportingNe()) {
                    if (sNe.getNodeRef().getValue().equals(phyNode.getNodeId().getValue())) {
                        location = sNe.getLocation();
                        found = true;

                        rackSequence = rack.getGlobalIdentify();
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                log.error("the phyNode hasn't find in related siteNode {}", phyNode.getNodeId().getValue());
            }
            suffix = rackSequence + location;
        } catch (Exception e) {
            suffix = String.valueOf(System.currentTimeMillis());
            log.error("error happen", e);
        }

        String friendlyName = buildFriendlyName(siteCode, vendorName, suffix);
        log.debug("node {} with friendlyName {}", phyNode.getNodeId().getValue(), friendlyName);
        return friendlyName;
    }

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    private ReentrantLock getLock(Node siteNode) {
        String siteId = siteNode.getNodeId().getValue();
        return LOCK_MAP.computeIfAbsent(siteId, k -> new ReentrantLock());
    }

    public Node updateFriendlyName(Node phyNode, Node siteNode) {
        ReentrantLock lock = getLock(siteNode);
        lock.lock();

        try {
            NodeBuilder nb = new NodeBuilder(phyNode);
            PhysicalBuilder pb = new PhysicalBuilder(phyNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical());
            boolean friendlyNameUpdated = false;
            if (pb.getFriendlyName() == null || pb.getFriendlyName().isEmpty() || pb.getFriendlyName().startsWith("Site-")) {
                pb.setFriendlyName(createFriendlyName(siteNode, phyNode));

                friendlyNameUpdated = true;
            }
            if (friendlyNameUpdated) {
                List<Property> propList;
                if (pb.getProperties() != null) {
                    propList = new ArrayList<>(pb.getProperties().getProperty());
                } else {
                    propList = new ArrayList<>();
                }
                propList.add(new PropertyBuilder().setName(PropKey_HostName)
                        .setKey(new PropertyKey(PropKey_HostName))
                        .setValue(pb.getFriendlyName())
                        .build());
                pb.setProperties(new PropertiesBuilder().setProperty(propList).build());

                Node1Builder n1b = new Node1Builder().setPhysical(pb.build());
                Node newNode = nb.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class, n1b.build()).build();
                return newNode;
            }
            return phyNode;
        } finally {
            lock.unlock();
        }
    }
}
