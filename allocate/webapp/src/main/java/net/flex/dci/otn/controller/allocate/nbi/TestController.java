/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.nbi;

import java.math.BigInteger;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
//import net.flex.dci.otc.optical.tool.MongoSpanLossProvider;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
//import net.flex.dci.otn.controller.allocate.network.bytedance.ase.ByteDanceSpec;
import net.flex.dci.otn.controller.allocate.test.MultiTests;
import net.flex.dci.otn.controller.allocate.test.SingleTests;
import net.flex.dci.otn.controller.allocate.test.TestEnv;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j

@RestController
public class TestController {


    @GetMapping(value = "/test/freq", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String checkFrequency() {
        try {
            ChangedObject changedObject = new ChangedObject();
            String siteLinkId = "SiteLink-Site-1950761424313782272#Ne-1951839897606295552#LINECARD-1-1#PORT-1-1-LINE-Site-1950761423458144256#Ne-1951839898197692416#LINECARD-1-1#PORT-1-1-LINE";
//            MongoSpanLossProvider provider = new MongoSpanLossProvider();
//            new ByteDanceSpec(siteLinkId).start();  //修改默认参数

            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            List<Link> siteLinkList = new ArrayList<>();
            siteLinkList.add(siteLink);
            List<Long> freeList = FrequencyAvailable.getFreeCentFrequency(siteLinkList, WDM_Band.C, GridType._150);  //业务OCH 是 C波段
            System.out.println(freeList.size());
            System.out.println(freeList);
            String msg = String.format("\nfree frequency size: %d\n %s", freeList.size(), freeList.toString());
            log.debug(msg);

            String ochLinkId = "OchLink-Site-1688835013895917568#Ne-1945579406936379392#LINECARD-1-7#PORT-1-7-L1-Site-1688835016085344256#Ne-1945579407162871808#LINECARD-1-7#PORT-1-7-L1";
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
            Link newOchLink = new LinkBuilder(ochLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                    new Link1Builder().setOch(new OchBuilder(ochLinkAttr)
                                    .setLowerFrequency(new FrequencyType(BigInteger.valueOf(192550000)))
                                    .setUpperFrequency(new FrequencyType(BigInteger.valueOf(192650000)))
                                    .build())
                            .build())
                    .build();
            SiteLinkOchUpdater updater = new SiteLinkOchUpdater(siteLink);
            List<Available> oldAva = siteLink.getAugmentation(Link1.class).getSite().getAvailable();
            updater.addNewOch(newOchLink);  //remove frequency ava, bandwidth, supportedLink...
            changedObject.addChangedSiteLink(updater.getSiteLink());
            siteLink = changedObject.getChangedSiteLink(siteLinkId);
            List<Available> newAva = siteLink.getAugmentation(Link1.class).getSite().getAvailable();
            List<Long> newFreeList = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), WDM_Band.C, GridType._150);
            msg = String.format("\nnew free frequency size: %d\n %s", newFreeList.size(), newFreeList);
            log.debug(msg);
            log.debug("add new och link, the sitelink ava will be change, \n old is {}, \n new is {}", oldAva, newAva);

            oldAva = newAva;
            updater.removeOch(newOchLink);
            changedObject.addChangedSiteLink(updater.getSiteLink());
            siteLink = changedObject.getChangedSiteLink(siteLinkId);
            newAva = siteLink.getAugmentation(Link1.class).getSite().getAvailable();

            List<Long> removeFreeList = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), WDM_Band.C, GridType._150);
            msg = String.format("\nremove och link and the  free frequency size: %d\n %s", removeFreeList.size(), removeFreeList);
            log.debug(msg);

            log.debug("add remove och link, the sitelink ava will be change, \n old is {}, \n new is {}", oldAva, newAva);
        } catch (Exception e) {
            log.error("error", e);
        }
        return "done";
    }

    @GetMapping(value = "/test/clean", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String cleanTestEnv() {
        TestEnv tester = new TestEnv();
        tester.clean();
        return "mongo DB cleaned";
    }

    @GetMapping(value = "/test/node", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createNode() {
        SingleTests tester = new SingleTests();
        tester.createSiteNode();
        return "createNode start";
    }

    //https://...../test/link?grid=0
    @GetMapping(value = "/test/link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createLink(int grid) {
        SingleTests tester = new SingleTests();
        tester.createSiteLink(grid);
        return "create grid=" + grid + " start";
    }

    //https://...../test/tunnel?number=100
    @GetMapping(value = "/test/tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createTunnel(int number) {
        SingleTests tester = new SingleTests();
        tester.createTunnels(number);
        return "create " + number + " tunnel start";
    }

    @GetMapping(value = "/test/multi", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String testMulti(Integer number) {
        MultiTests tester = new MultiTests();
        tester.check(number);
        return "start multi thread test";
    }

    //https://...../test/tunnel?grid=0&number=100
//    @GetMapping(value = "/test/tunnel", produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public String startTest1(int grid, int number) {
//        SingleTests tester = new SingleTests();
//        tester.createTunnels(grid, number);
//        return "create " + number + " tunnel over grid=" + grid + " siteLink start";
//    }
}
