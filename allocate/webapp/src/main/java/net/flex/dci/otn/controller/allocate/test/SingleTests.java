/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.test;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.test.link.SiteLinkTests;
import net.flex.dci.otn.controller.allocate.test.link.TunnelTests;
import net.flex.dci.otn.controller.allocate.test.node.SiteNodeTests;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.util.List;
import java.util.Random;

@Slf4j
public class SingleTests {

  private SiteNodeDao siteNodeDao;

  public SingleTests() {
    this.siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
  }

  private static String LPORTRATE_200G = "common-otn-types:prot-OTUc2";
  private static String LPORTRATE_400G = "common-otn-types:prot-OTUc4";
  private static String LPORTRATE_100G = "common-otn-types:prot-OTU4";

  public void createTunnels(int number) {
    List<Node> nodeList = getSiteNodes();
    String a = getNodeId(nodeList.get(0));
    String z = getNodeId(nodeList.get(nodeList.size() - 1));

    TunnelTests tester = new TunnelTests();
    tester.case1(a, z, "T2X4C8", LPORTRATE_400G, number);
    tester.case1(a, z, "T2X4C8", LPORTRATE_200G, number);
    tester.case1(a, z, "T2X2C4", LPORTRATE_200G, number);
    tester.case1(a, z, "T2X2C4", LPORTRATE_100G, number);

  }


  private void createSiteLinks(int grid) {
    List<Node> nodeList = getSiteNodes();
    //case1 需要2个siteNode
    //case2 需要3个siteNode
    //case3 需要3个siteNode
    //case4 需要5个siteNode

    String aId = getNodeId(nodeList.get(0));
    String zId = getNodeId(nodeList.get(nodeList.size() - 1));

    SiteLinkTests tester = new SiteLinkTests();
//    if (nodeList.size()>=2) {
//      tester.case1(aId, zId, grid);
//    }
//
//    if (nodeList.size()>=3) {
//      tester.case2(aId, getNodeId(nodeList.get(1)), zId, grid);
//    }
//
//    if (nodeList.size()>=3) {
//      tester.case3(aId, getNodeId(nodeList.get(1)), zId, grid);
//    }

    if (nodeList.size()>=5) {
      tester.case4(aId, getNodeId(nodeList.get(1)), getNodeId(nodeList.get(2)), getNodeId(nodeList.get(3)), zId, grid);
    }
  }

  private String getNodeId(Node node) {
    return node.getNodeId().getValue();
  }

  private List<Node> getSiteNodes() {
    return siteNodeDao.listSiteNodes();
  }

  private void createSites() {
    SiteNodeTests tester = new SiteNodeTests();
    for(int i=1; i<6; i++) {
      tester.case1();
    }
  }

  public void createSiteNode() {
    createSites();
  }

  public void createSiteLink(int grid) {
    createSiteLinks(grid);
  }

  public String createTunnels(int grid, int number) {
    SiteLinkDao dao = SpringBeanFinder.getBean(SiteLinkDao.class);
    List<Link> linkList = dao.getSiteLinks();
    for (Link link : linkList) {
      Site siteAttr = link.getAugmentation(Link1.class).getSite();
      if (siteAttr.getGrid().getIntValue() == grid) {
//        if (link.get)
      }
    }
    return "";
  }
}
