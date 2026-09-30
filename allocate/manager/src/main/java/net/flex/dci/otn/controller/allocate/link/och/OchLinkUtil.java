/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.och;

import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YYX
 * @date 12/6/2021
 */
public class OchLinkUtil {
  public static Available addAvailable(Available oldAva, String odujSlot) {
    String  avaStr;
    if (oldAva.getAvailableOdujSlot() == null || oldAva.getAvailableOdujSlot().isEmpty()) {
      avaStr = odujSlot;
    } else {
      //添加容错性，检查需要加入的是否已经存在
//      avaStr = oldAva.getAvailableOdujSlot() + "," + odujSlot;

      List<String> idsList = Arrays.asList(oldAva.getAvailableOdujSlot().split("-"))
              .stream().map(s -> s.trim()).collect(Collectors.toList());
      Set<String> idsSet = new HashSet<>();
      idsSet.addAll(idsList);
      idsSet.add(odujSlot);
      avaStr = idsSet.stream().map(n -> String.valueOf(n)).collect(Collectors.joining("-"));

    }
    Available newAva = new AvailableBuilder(oldAva).setAvailableOdujSlot(avaStr).build();
    return newAva;
  }

  public static Available newAvailable(OduGranularity odujLevel, String odujSlot) {
    Available newAva = new AvailableBuilder()
            .setSupportedOduj(odujLevel)
            .setKey(new AvailableKey(odujLevel))
            .setAvailableOdujSlot(odujSlot)
            .build();
    return newAva;
  }

  public static Link addOdujAvailable(Link ochLink, Integer pos, Available newAva) {
    Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
    List <Available> newAvaList = new ArrayList<>();
    newAvaList.addAll(ochLinkAttr.getAvailable());

    if (pos == null) {
      newAvaList.add(newAva);
    } else {
      newAvaList.add(pos, newAva);
    }

    Link link = new LinkBuilder(ochLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setOch(new OchBuilder(ochLinkAttr).setAvailable(newAvaList).build())
                    .build())
            .build();

    return link;
  }

  public static Link setSupportedTunnel(Link ochLink, List<SupportedTunnel> newList) {
    Link1 link1 = ochLink.getAugmentation(Link1.class);

    Link link = new LinkBuilder(ochLink)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder()
                    .setSupportedTunnel(newList)
                    .build())
            .build();

    return link;
  }

  /**
   * WSS 引入后，TP 就需要检查route了
   * @param ochLink
   * @return
   */
  public static List<String> getLinkTp(Link ochLink) {
    List<String> tpIdList = new ArrayList<>();
    tpIdList.add(ochLink.getSource().getSourceTp().getValue());
    tpIdList.add(ochLink.getDestination().getDestTp().getValue());

    return tpIdList;
  }

  public static List<String> getSiteLink(Link ochLink) {
    List<String> idList = new ArrayList<>();
    for(SupportingLink sl : ochLink.getSupportingLink()) {
      if (SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue())) {
        idList.add(sl.getLinkRef().getValue());
      }
    }
    return idList;
  }

}
