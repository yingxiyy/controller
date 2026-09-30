/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
public class TunnelFriendlyName {
  //源Tpc名称-目的Tpc名称-端口序号

  public static String buildFriendlyName(Node srcNode, Node dstNode, String stpId, String dtpId, String lPortId, OduGranularity odukType) {
    log.debug("start build tunnel friendly name");

    Physical phySrcNode = srcNode.getAugmentation(Node1.class).getPhysical();
    Physical phyDstNode = dstNode.getAugmentation(Node1.class).getPhysical();
    String srcNodeName = phySrcNode.getFriendlyName();
    String dstNodeName = phyDstNode.getFriendlyName();

    int slotNumber = PhysicalTpIdNamingRule.getSlotId(lPortId);
    int cPortNumber = PhysicalTpIdNamingRule.getPortNumberInteger(stpId);
    int factor = getFactor(odukType);
    int sequence = (slotNumber - 1) * factor + cPortNumber;
    String friendlyName = srcNodeName + "==" + dstNodeName + "-" + sequence;

    //method2 源Tpc名称==目的Tpc名称#Port-1-1-C1
    friendlyName = srcNodeName + "#" + PhysicalTpIdNamingRule.getPurePortNameByTpId(stpId)
                  + "==" +
                  dstNodeName + "#" + PhysicalTpIdNamingRule.getPurePortNameByTpId(dtpId);

    log.debug("tunnel friendly name is {}", friendlyName);
    return friendlyName;
  }

  private static int getFactor(OduGranularity odukType) throws CommonException {
    switch (odukType) {
      case Odu4x2:
        return 2;
      case Odu4x3:
        return 3;
      case Odu4x4:
        return 4;
      case Odu4:
        return 1;
      case Odu4x8:
        return 8;
      case Odu4x6:
        return 6;
    }

    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "doesn't know how to covert odukType:" + odukType);
  }
}
