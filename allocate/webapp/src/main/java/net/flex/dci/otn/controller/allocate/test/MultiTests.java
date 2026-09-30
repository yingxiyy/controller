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
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.serialization.Test;
import net.flex.dci.otn.controller.allocate.test.link.SiteLinkTests;
import net.flex.dci.otn.controller.allocate.test.link.TunnelTests;
import net.flex.dci.otn.controller.allocate.test.node.SiteNodeTests;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

import java.util.List;
import java.util.Random;

@Slf4j
public class MultiTests {

  private List<Node> siteNodeList;

  public MultiTests() {
    SiteNodeDao dao = SpringBeanFinder.getBean(SiteNodeDao.class);
    siteNodeList = dao.listSiteNodes();
  }

  private static String LPORTRATE_200G = "common-otn-types:prot-OTUc2";
  private static String LPORTRATE_400G = "common-otn-types:prot-OTUc4";
  private static String LPORTRATE_100G = "common-otn-types:prot-OTU4";

  public void check(Integer number) {
    if (number == null)
      number = 100;
    prepare();

    for (int i=0; i<number; i++) {
      new Runnable() {
        @Override
        public void run() {
          anyAction();
        }
      }.run();
    }
  }

  private void anyAction() {
    SingleTests tester = new SingleTests();
    Random random = new Random();
    int cmdNo = random.nextInt(6);
    switch (cmdNo) {
      case 0:
        createSiteLink(0);
        break;
      case 1:
        createSiteLink(50);
        break;
      case 2:
        createSiteLink(75);
        break;
      case 4:
        createTunnel();
        break;
//      case 5:
//        deleteTunnel();
//        Random tunnelRandom = new Random();
//        int tunnelNumber = random.nextInt(100);
//        createTunnels(tunnelNumber + 1);
//        break;


    }
  }

  private void createTunnel() {
    try {
      Random tunnelRandom = new Random();
      int number = tunnelRandom.nextInt(100) + 1;

      int aPos = getASiteNode();
      int zPos = getASiteNode(aPos);
      String a = getNodeId(siteNodeList.get(aPos));
      String z = getNodeId(siteNodeList.get(zPos));

      TunnelTests tester = new TunnelTests();
      tester.case1(a, z, "T2X4C8", LPORTRATE_400G, number);
      tester.case1(a, z, "T2X4C8", LPORTRATE_200G, number);
      tester.case1(a, z, "T2X2C4", LPORTRATE_200G, number);
      tester.case1(a, z, "T2X2C4", LPORTRATE_100G, number);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void createSiteLink(int grid) {
    try {
      SiteLinkTests test = new SiteLinkTests();

      int aPos = getASiteNode();
      int zPos = getASiteNode(aPos);
      String a = getNodeId(siteNodeList.get(aPos));
      String z = getNodeId(siteNodeList.get(zPos));
      test.case1(a, z, grid);

      int bPos = getASiteNode(aPos, zPos);
      String b = getNodeId(siteNodeList.get(bPos));
      test.case2(a, b, z, grid);
      test.case3(a, b, z, grid);

      int cPos = getASiteNode(aPos, bPos, zPos);
      int dPos = getASiteNode(aPos, bPos, cPos, zPos);
      String c = getNodeId(siteNodeList.get(cPos));
      String d = getNodeId(siteNodeList.get(dPos));
      test.case4(a, b, c, d, z, grid);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getNodeId(Node node) {
    return node.getNodeId().getValue();
  }

  private void prepare() {
    log.debug("prepare start, clean at first and then basic data");


    long start, end;
    start = System.currentTimeMillis();

    TestEnv env = new TestEnv();
    env.clean();

    SingleTests tester = new SingleTests();
    tester.createSiteNode();
    tester.createSiteLink(0);
    tester.createTunnels(1);

    TunnelDao dao = SpringBeanFinder.getBean(TunnelDao.class);
    while (true) {
      List<Tunnel> tunnelList = dao.listTunnels();
      if (tunnelList != null && tunnelList.size() > 0) {
        break;
      } else {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    end = System.currentTimeMillis();
    log.debug("prepare done, take {}", end-start);
  }

  private int getASiteNode(int... args) throws Exception {
    Random r = new Random();
    if (args.length == 0)
      return r.nextInt(siteNodeList.size());

    if (args.length == siteNodeList.size()) {
      log.debug("all nodes been allocat. cannot find new one");
      throw new RuntimeException("all nodes been allocat. cannot find new one");
    }

    int retry = 100;
    while(retry > 0) {
      retry--;
      int newPos = r.nextInt(siteNodeList.size());
      for (int i = 0; i < args.length; i++) {
        if (args[i] == newPos)
          break;
      }
      return newPos;
    }

    log.debug("cannot find one suitable node");
    throw new RuntimeException("cannot find one suitable node");
  }


}
