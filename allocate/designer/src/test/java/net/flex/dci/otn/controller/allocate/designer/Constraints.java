/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

public class Constraints {

    public static final String SITE1 = "Site-1643017481047";
    public static final String SITE2 = "Site-1643017493568";
    public static final String SITE3 = "Site-1643017487480";
    public static final String SITE4 = "Site-4";
    public static final String SITE5 = "Site-5";
    public static final String NON_PROTECTED_SITE_TYPE = "OMS";
    public static final String PROTECTED_SITE_TYPE = "OMSP";
    public static final String ILA_SITE_TYPE = "ILA";
    public static final int CMUX_GRID = 0;
    public static final int GRID_50 = 50;
    public static final int GRID_75 = 75;
    public static final String VENDOR_NAME = "II-VI";
    public static final String VENDOR_TYPE = "OPC-4";


    public static final String ILA_SITE="Site-1643017493568";

    public static List<Equipments> getEquipments(
            Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments();
    }

    public static List<CrossConnections> getXCs(
            Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
    }

    public static List<InternalLinks> getInternalLinks(
            Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
    }
}
