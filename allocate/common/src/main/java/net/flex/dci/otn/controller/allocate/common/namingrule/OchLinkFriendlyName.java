/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;

import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;

/**
 * @author YYX
 * @version 1.0
 */
public class OchLinkFriendlyName {

    //method2 源Tpc名称-目的Tpc名称#Port-1-1-L1
    public static String buildFriendlyName(Node srcNode, Node dstNode, String srcTp, String dstTp) {
        return  srcNode.getAugmentation(Node1.class).getPhysical().getFriendlyName() + "#" +
                PhysicalTpIdNamingRule.getPurePortNameByTpId(srcTp)+ "==" +
                dstNode.getAugmentation(Node1.class).getPhysical().getFriendlyName() + "#" +
                PhysicalTpIdNamingRule.getPurePortNameByTpId(dstTp);

    }
}
