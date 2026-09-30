/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.site;

import java.util.*;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;


/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
@Data
public class SiteLinkOchUpdater {

  private Link siteLink;
  public SiteLinkOchUpdater(Link siteLink) {
    this.siteLink = siteLink;
  }

  /**
   * when add a new OCH, site link will change
   * 1. available frequency
   * 2. suported link
   * 3. bandwidth
   *
   * @param ochLink
   * @return
   */
  public void addNewOch(Link ochLink) {
    Och linkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
    updateSiteLinkAvailable(linkAttr.getLowerFrequency(), linkAttr.getUpperFrequency(), false);
    addOchLinkToSiteLinkSupportedLink(ochLink);

    updateChangeSiteLinkBandwidth(-1);
  }

  public void removeOch(Link ochLink) {
    log.debug("remove OCH Link: {}", ochLink.getLinkId().getValue());

    Och linkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

    updateSiteLinkAvailable(linkAttr.getLowerFrequency(), linkAttr.getUpperFrequency(), true);
    removeOchLinkFromSiteLinkSupportedLink(ochLink.getLinkId().getValue());
    updateChangeSiteLinkBandwidth(+1);
  }

  private void updateChangeSiteLinkBandwidth(int delta) {
    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
    siteLink = new LinkBuilder(siteLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setSite(new SiteBuilder(siteLinkAttr)
                            .setBandwidth(String.valueOf(Integer.valueOf(siteLinkAttr.getBandwidth()) + delta))
                            .build()
                    ).build()
            ).build();
  }

  private void removeOchLinkFromSiteLinkSupportedLink(String ochLinkId) {
    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
    List newSupportedLinkList = new ArrayList();
    newSupportedLinkList.addAll(siteLinkAttr.getSupportedLink());
    Iterator<SupportedLink> iter = newSupportedLinkList.iterator();
    while (iter.hasNext()) {
      SupportedLink sl = iter.next();
      if (sl.getLinkRef().getValue().equals(ochLinkId)) {
        iter.remove();
        break;
      }
    }

    siteLink = new LinkBuilder(siteLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setSite(new SiteBuilder(siteLinkAttr)
                            .setSupportedLink(newSupportedLinkList)
                            .build()
                    ).build()
            ).build();
  }

  private void updateSiteLinkAvailable(FrequencyType lowerFrequency, FrequencyType upperFrequency, boolean increase) {
    Available ava = new AvailableBuilder()
            .setLowerFrequency(lowerFrequency)
            .setUpperFrequency(upperFrequency)
            .setKey(new AvailableKey(lowerFrequency))
            .build();
    FrequencyAvailable frequencyAvailable = new FrequencyAvailable(siteLink);
    if (increase)
      frequencyAvailable.add(ava);
    else
      frequencyAvailable.remove(ava);


    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
    siteLink = new LinkBuilder(siteLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setSite(new SiteBuilder(siteLinkAttr)
                            .setAvailable(frequencyAvailable.getAvailableList())
                            .build()
                    ).build()
            ).build();
  }

  private void addOchLinkToSiteLinkSupportedLink(Link ochLink) {
    TopologyId ochTopoId = new TopologyId(TopoNameConstants.Och_Topo_Key);

    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
    List<SupportedLink> newSupportedLinkList = new ArrayList();
    newSupportedLinkList.addAll(siteLinkAttr.getSupportedLink());
    newSupportedLinkList.add(new SupportedLinkBuilder()
            .setTopologyRef(ochTopoId)
            .setLinkRef(ochLink.getLinkId())
            .build());


    siteLink = new LinkBuilder(siteLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setSite(new SiteBuilder(siteLinkAttr)
                            .setSupportedLink(newSupportedLinkList)
                            .build()
                    ).build()
            ).build();
  }


  public boolean checkAndRemove() {
    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
    if (siteLinkAttr.getPlaneName().equals(Constant.VIRTUAL_PLANE)) {
      if (siteLinkAttr.getSupportedLink() == null || siteLinkAttr.getSupportedLink().isEmpty()) {
        log.debug("we can remove the virual site link now");
        SiteLinkRemover siteLinkRemover = new SiteLinkRemover();
        siteLinkRemover.setQuite(true);

        RemoveLinkInput removeData = new RemoveLinkInputBuilder()
                .setLinkId(siteLink.getLinkId().getValue())
                .setForce(true)
                .setForceDb(true)
                .build();
        siteLinkRemover.doIt(removeData);
        log.debug("the virual site link removed ()", siteLink.getLinkId().getValue());
        return true;
      }
    }
    return false;
  }
}
