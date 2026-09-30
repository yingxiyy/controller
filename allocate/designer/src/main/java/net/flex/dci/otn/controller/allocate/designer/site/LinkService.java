/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.*;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.site.model.LinkOutput;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class LinkService {

    public static final String MUXPANEL = "MUXPANEL";
    public static final String MPO4 = "MPO4";
    @Autowired
    private LinkRepo linkRepo;

    @Autowired
    private NEInfoConfig neInfoConfig;


    public InternalLinks createInternalLink(String nodeId, Link link) {
        return linkRepo.createInternalLink(nodeId, link);
    }

    private LinkOutput createLinks(String nodeId, Map<String, List<ExternalLinkTo>> fromToMap, Map<String, String> srcPortNameTpIdMap, Map<String, String> destPortNameIdTpMap, Set<String> busyTpIds,
            LinkType linkType)
            throws NeDesignerException {
        //output
//        Set<String> busyTpIds = new HashSet<>();
        List<Link> links = new ArrayList<>();
        List<InternalLinks> internalLinks = new ArrayList<>();

        try {
            for (Entry<String, List<ExternalLinkTo>> externalLinkEntry : fromToMap.entrySet()) {
                for (ExternalLinkTo externalLinkTo : externalLinkEntry.getValue()) {
                    String srcPortName = externalLinkEntry.getKey();
                    String destPortName = externalLinkTo.getPort();
                    List<String> fromNameList = NeInfoUtil.getNameList(srcPortName);
                    List<String> toNameList = NeInfoUtil.getNameList(destPortName);

                    int length = fromNameList.size() > toNameList.size() ? fromNameList.size() : toNameList.size();

                    /**
                     * Scenario:
                     *
                     * 1. from: "SIG", to: "A,B":  two link "<SIG,A>, <SIG,B>"
                     *
                     * 2. from: "MPO?,1,8,1", to: "MPO?,1,8,1": return 8 entry: "<MPO1,MPO1>,<MPO2,MPO2>...<MPO8,MPO8>"
                     */
                    for (int i = 0; i < length; i++) {
                        String from, to;
                        try {
                            from = fromNameList.get(i);
                        } catch (IndexOutOfBoundsException e) {
                            from = fromNameList.get(0);
                        }
                        try {
                            to = toNameList.get(i);
                        } catch (IndexOutOfBoundsException e) {
                            to = toNameList.get(0);
                        }

                        String srcTpId = srcPortNameTpIdMap.get(from);
                        String destTpId = destPortNameIdTpMap.get(to);
                        if (srcTpId == null) {
                            continue;
                        }
                        if (destTpId == null) {
                            continue;
                        }
                        if (busyTpIds.contains(srcTpId) || busyTpIds.contains(destTpId)) {
                            log.debug("Not create link from {} to {}, because link exists already.", from, to);
                            continue;
                        }
                        busyTpIds.add(srcTpId);
                        busyTpIds.add(destTpId);
//                        srcPortNameTpIdMap.remove(from);
//                        destPortNameIdTpMap.remove(to);

                        Link link = linkRepo.createLink(srcTpId, destTpId, linkType);
                        links.add(link);
                        internalLinks.add(linkRepo.createInternalLink(nodeId, link));
                    }
                }
            }

        } catch (Exception e) {
            String msg = "Failed to create oms Link";
            log.error(msg, e);
            throw new NeDesignerException(msg, e);
        }

        return LinkOutput.builder().busyTpIds(busyTpIds).links(links).internalLinks(internalLinks).build();
    }

    public LinkOutput createOmsLinks(String nodeId, Map<String, List<ExternalLinkTo>> fromToMap, Map<String, String> srcPortNameTpIdMap, Map<String, String> destPortNameIdTpMap, Set<String> busyIds)
            throws NeDesignerException {
        return createLinks(nodeId, fromToMap, srcPortNameTpIdMap, destPortNameIdTpMap, busyIds, LinkType.OmsLink);
    }

    public LinkOutput createOtsLinks(String nodeId, Map<String, List<ExternalLinkTo>> fromToMap, Map<String, String> srcPortNameTpIdMap, Map<String, String> destPortNameIdTpMap, Set<String> busyIds)
            throws NeDesignerException {
        return createLinks(nodeId, fromToMap, srcPortNameTpIdMap, destPortNameIdTpMap, busyIds, LinkType.OtsLink);

    }

    public LinkOutput createCLLinks(String nodeId, Map<String, List<ExternalLinkTo>> fromToMap, Map<String, String> srcPortNameTpIdMap, Map<String, String> destPortNameIdTpMap, Set<String> busyIds)
            throws NeDesignerException {

        return createLinks(nodeId, fromToMap, srcPortNameTpIdMap, destPortNameIdTpMap, busyIds, LinkType.CableLink);
    }

    public Link createMpoLink(String muxMpoTp, String expTpId) throws NeDesignerException {

        return linkRepo.createLink(muxMpoTp, expTpId, LinkType.OmsLink);

    }

    public List<AddDropLink> createAddDropLinks(List<Link> mpoLinks) {
        List<AddDropLink> result = new ArrayList<>();
        for (Link mpoLink : mpoLinks) {
            AddDropLink addDroplink = new AddDropLinkBuilder().setLinkRef(mpoLink.getLinkId().getValue())
                    .setKey(new AddDropLinkKey(mpoLink.getLinkId().getValue()))
                    .setConnnectorType(EquipType.MUXPANEL)
                    .build();
            result.add(addDroplink);
        }
        return result;
    }

    public Link createLink(String linkId, LinkType linkType) throws NeDesignerException {
        String srcTpId= PhysicalLinkIdNamingRule.getTpAId(linkId);
        String dstTpId= PhysicalLinkIdNamingRule.getTpZId(linkId);
        return linkRepo.createLink(srcTpId, dstTpId, linkType);
    }
}
