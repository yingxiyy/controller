/*
*  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
*
*  This program and the accompanying materials are made available under the
*  terms of the Eclipse Public License v1.0 which accompanies this distribution,
*  and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package net.flex.dci.otn.controller.allocate.node.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteRackIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.common.namingrule.SiteNodeFriendlyName;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.tp.attributes.SiteBuilder;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
* @author YYX
* @version 1.0
*/
@Slf4j
public class SiteNodeCorrelateResource {

    //修改一下， 考虑到最上面的OPC网元需要再重用的情况下有可能放两个MUX， 36，37 放OPC， 38，39放MUX1， 40，41放MUX2
    private static final int RACK_START_U = 36;
    private static final int RACK_STEP_U = 3;
    private Node siteNode;

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    private ReentrantLock getLock() {
        String siteId = siteNode.getNodeId().getValue();
        return LOCK_MAP.computeIfAbsent(siteId, k -> new ReentrantLock());
    }

    private PhyNodeDao getPhyNodeDao() {
        return SpringBeanFinder.getBean(PhyNodeDao.class);
    }

    public SiteNodeCorrelateResource(Node siteNode) {
        log.debug("correlate siteNode: {}", siteNode.getNodeId().getValue());
        this.siteNode = siteNode;
    }

//一个rack 可以放14个网元， 一个OPC放最上面（40），其他都是TPC

    /**
     * TPC4/OPT4 设备插入rack 如果这个node 已经加入了，不会重复加
     *
     * @param siteLinkId
     * @param node
     * @return
     */
    public SiteNodeCorrelateResource insertRack(String siteLinkId, Node node) {
        ReentrantLock lock = getLock();
        lock.lock();

        try {
            Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();

            log.debug("insertRack with siteLinkId, phyNodeId on SiteNode: \n{}\n{}\n{}({})",
                    siteLinkId, node.getNodeId().getValue(), siteNode.getNodeId().getValue(), siteNodeAttr.getFriendlyName());

            Physical phyNodeAttr = node.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                    .getPhysical();
            if (phyNodeAttr != null && phyNodeAttr.getPlaneName() != null && phyNodeAttr.getPlaneName()
                    .equals(Constant.VIRTUAL_PLANE)) {
                log.debug("this is one virtual OPC NE, doesn't insert on rack");
                return this;
            }

            insert2SiteNode(siteLinkId, node);

            return this;
        } finally {
            lock.unlock();
        }
    }

    public SiteNodeCorrelateResource insertIntoSiteLinkRack(String siteLinkId, String rackName,
            Node node) {
        if (siteLinkId == null || siteLinkId.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "without siteLinkId, cannot identify which rack to insert");
        }

        ReentrantLock lock = getLock();
        lock.lock();
        try {
            Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();
            boolean rackExists = siteNodeAttr.getSupportingRack() != null
                    && siteNodeAttr.getSupportingRack().stream()
                            .anyMatch(rack -> rack.getRackId() != null
                                    && rack.getRackId().getValue().contains(siteLinkId));
            if (!rackExists) {
                newRack(siteLinkId, rackName);
            }
            return insertRack(siteLinkId, node);
        } finally {
            lock.unlock();
        }
    }

    private String getLocationInRack(SupportingRack rack) {
        Set<Integer> installed2U = new HashSet<>();
        for (SupportingNe sNe : rack.getSupportingNe()) {
            installed2U.add(Integer.parseInt(sNe.getLocation()));
        }

        for (int i = 0; ; i++) {
            int candidate = RACK_START_U - (RACK_STEP_U * i);
            if (!installed2U.contains(candidate)) {
                return String.valueOf(candidate);
            }
        }
    }

    //应该只有创建复用段的时候才会创建
    public SiteNodeCorrelateResource newRack(String siteLinkId, String rackName) {
        ReentrantLock lock = getLock();
        lock.lock();

        try {
            Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();
            log.debug("newRack with siteLinkId, rackName on SiteNode: \n{}\n{}\n{}({})",
                    siteLinkId, rackName, siteNode.getNodeId().getValue(), siteNodeAttr.getFriendlyName());

            SupportingRack rack = createRack(siteLinkId, rackName, SiteNodeFriendlyName.getRackGlbalID(siteNode));

            List<SupportingRack> rackList = siteNodeAttr.getSupportingRack() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(siteNodeAttr.getSupportingRack());

            rackList.add(rack);

            siteNode = new NodeBuilder(siteNode).addAugmentation(Node1.class, new Node1Builder()
                            .setSite(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(siteNodeAttr)
                                    .setSupportingRack(rackList)
                                    .build())
                            .build())
                    .build();
            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * base on phyNode find out which rack we want to update
     * 这个用在同一个网元跨多个复用段的情况，
     *
     * @param siteLinkId
     * @param siteLinkName
     * @param phyNode
     * @return
     */
    public SiteNodeCorrelateResource updateRack(String siteLinkId, String siteLinkName, Node phyNode) {
        if (siteLinkId == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "without siteLinkId, cannot identify which rack to insert");
        }

        ReentrantLock lock = getLock();
        lock.lock();

        try {
            String phyNodeId = phyNode.getNodeId().getValue();
            Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();
            log.debug("updateRack with siteLinkId, siteLinkName, phyNodeId on SiteNode: \n updateRack: {}\n{}\n{}\n{}({})",
                    siteLinkId, siteLinkName, phyNodeId, siteNode.getNodeId().getValue(), siteAttr.getFriendlyName());

            List<SupportingRack> rackList = siteAttr.getSupportingRack() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(siteAttr.getSupportingRack());

            // ---------------------------------------------------
            // find current rack by siteLinkId
            // ---------------------------------------------------

            SupportingRack oldRack = findRack(phyNodeId, rackList);

            // ===================================================
            // CASE 1:
            // rack found -> rename rack
            // ===================================================

            if (oldRack != null) {

                String newRackId = SiteRackIdNamingRule.extractKey(oldRack.getRackId().getValue()) + "," + siteLinkId;

                String newRackName = oldRack.getFriendlyName() + "," + siteLinkName;

                SupportingRack updatedRack = createRack(newRackId, newRackName, oldRack.getGlobalIdentify());

                updatedRack.getSupportingNe().addAll(oldRack.getSupportingNe());

                List<SupportingRack> newRackList = rackList.stream().map(rack -> {

                    if (rack.getRackId().getValue().equals(oldRack.getRackId().getValue())) {
                        return updatedRack;
                    }

                    return rack;
                }).collect(Collectors.toList());

                rebuildSiteNode(newRackList, siteNode.getSupportingNode());

                return this;
            }

            // ===================================================
            // CASE 2:
            // rack not found -> create new rack
            // ===================================================

            SupportingRack newRack = createRack(siteLinkId, siteLinkName, SiteNodeFriendlyName.getRackGlbalID(siteNode));

            List<SupportingNe> neList = new ArrayList<>();

            neList.add(new SupportingNeBuilder()
                    .setNodeRef(new NodeId(phyNodeId))
                    .setLocation(getLocationInRack(newRack))
                    .setKey(new SupportingNeKey(new NodeId(phyNodeId)))
                    .build());

            newRack = new SupportingRackBuilder(newRack)
                    .setSupportingNe(neList)
                    .build();

            rackList.add(newRack);

            // ---------------------------------------------------
            // append supporting node if absent
            // ---------------------------------------------------

            List<SupportingNode> supportingNodeList = siteNode.getSupportingNode() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(siteNode.getSupportingNode());

            boolean supportingNodeExist = supportingNodeList.stream()
                    .anyMatch(sn -> phyNodeId.equals(sn.getNodeRef().getValue()));

            if (!supportingNodeExist) {
                supportingNodeList.add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId(phyNodeId))
                        .setKey(new SupportingNodeKey(
                                new NodeId(phyNodeId),
                                new TopologyId(TopoNameConstants.Phy_Topo_Key)))
                        .build());
            }

            rebuildSiteNode(rackList, supportingNodeList);

            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 当siteLinkId, siteLinkName 都有值的时候就是rack的ID，名称都需要修改
     *
     * @param siteLinkId
     * @param siteLinkName
     * @param phyNodeId
     * @return
     */

    /**
     * remove NE from rack
     *
     * siteLinkId != null:
     *      update rack id/name
     *
     * siteLinkId == null:
     *      only remove NE
     */
    public SiteNodeCorrelateResource updateRack_remove(String siteLinkId, String siteLinkName, String phyNodeId) {
        ReentrantLock lock = getLock();
        lock.lock();

        try {
            Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();

            log.debug("updateRack_remove with siteLinkId, siteLinkName, phyNodeId on SiteNode: \n removeFromRack: {}\n{}\n{}\n{}({})",
                    siteLinkId, siteLinkName, phyNodeId, siteNode.getNodeId().getValue(), siteAttr.getFriendlyName());

            List<SupportingRack> oldRackList = siteAttr.getSupportingRack() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(siteAttr.getSupportingRack());

            // ---------------------------------------------------
            // remove supporting node
            // ---------------------------------------------------

            List<SupportingNode> newSupportingNode = siteNode.getSupportingNode() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(siteNode.getSupportingNode());

            newSupportingNode.removeIf(sn -> phyNodeId.equals(sn.getNodeRef().getValue()));

            // ---------------------------------------------------
            // find target rack
            // ---------------------------------------------------

            SupportingRack targetRack = findRack(phyNodeId, oldRackList);

            if (targetRack == null) {
                log.error("cannot find the ne in all racks {}", phyNodeId);

                oldRackList = repairRack(oldRackList);
                newSupportingNode = repairSupportingNode(newSupportingNode);

                rebuildSiteNode(oldRackList, newSupportingNode);

                return this;
            }

            // ---------------------------------------------------
            // remove NE from rack
            // ---------------------------------------------------

            String targetRackId = targetRack.getRackId().getValue();
            oldRackList.removeIf(x->x.getRackId().getValue().equals(targetRackId));

            targetRack = removeNeFromRack(targetRack, phyNodeId);
            if (targetRack.getSupportingNe().isEmpty()) {
                log.debug("the rack is empty now after remove, so remove rack together {}", targetRack.getFriendlyName());
            } else {
                oldRackList.add(targetRack);
            }
            rebuildSiteNode(oldRackList, newSupportingNode);

            return this;
        } finally {
            lock.unlock();
        }
    }

    private List<SupportingRack> repairRack(List<SupportingRack> rackList) {
        rackList.removeIf(checkingRack -> checkingRack.getSupportingNe() == null || checkingRack.getSupportingNe().isEmpty());

        //相似的bug, 不确定那里引入的，rack上的网元已经被删除了，但是rack 的supportingNE中还有值
        rackList = rackList.stream().map(checkingRack -> {
            List<SupportingNe> neList = checkingRack.getSupportingNe();
            neList.removeIf(supportingNe -> {
                if (getPhyNodeDao().existsCfgNode(supportingNe.getNodeRef().getValue())) {
                    return false; //normal device, do not remove
                } else {
                    log.error("node NOT existed, I remove it from rack: {}", supportingNe.getNodeRef().getValue());
                    return true;
                }
            });

            if (neList.isEmpty()) {
                log.error("the rack without any ne will be removed. {}", checkingRack.getFriendlyName());
                return null;
            }
            return new SupportingRackBuilder(checkingRack).setSupportingNe(neList).build();
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return rackList;
    }

    private SupportingRack findRack(String phyNodeId, List<SupportingRack> rackList) {
        if (rackList == null) {
            return null;
        }

        for (SupportingRack rack : rackList) {
            if (rack.getSupportingNe() == null) {
                continue;
            }

            boolean found = rack.getSupportingNe().stream()
                .anyMatch(ne -> phyNodeId.equals(ne.getNodeRef().getValue()));

            if (found) {
                return rack;
            }
        }

        return null;
    }

    private SupportingRack removeNeFromRack(SupportingRack rack, String phyNodeId) {
        List<SupportingNe> newNeList = new ArrayList<>(rack.getSupportingNe());
        newNeList.removeIf(x->x.getNodeRef().getValue().equals(phyNodeId));

        return new SupportingRackBuilder(rack)
                .setSupportingNe(newNeList)
                .build();
    }

    private List<SupportingNode> repairSupportingNode(List<SupportingNode> supportingNode) {
        List<SupportingNode> newList = new ArrayList<>(supportingNode);
        newList.removeIf(sn -> {
            if (getPhyNodeDao().existsCfgNode(sn.getNodeRef().getValue())) {
                log.debug("node existed: {}", sn.getNodeRef().getValue());
                return false; //normal device, do not remove
            } else {
                log.debug("node NOT existed: {}", sn.getNodeRef().getValue());
                return true;
            }
        });
        return newList;
    }


    /**
     * 对于OPC网元 每一个OPC网元就是一个Rack，且放在rack的最上面，（MUX最上面，然后网元本身）
     * 一个OTS最多支持96波，一个96波支持2条业务，一张OT板卡可以支持4条业务，一个TPC网元支持4张OT板卡
     * 所以一个OT板卡占用2条波道，一个TPC网元占用8个波道，一个OPC的96波支持12个TPC网元
     * TPC网元1U，OPC网元+MUX板卡2U，网元间间隔1U。所以我们定义RACK的高度为30U
     *
     * @param rackName
     * @return
     */
    private SupportingRack createRack(String siteLinkId, String rackName, String globalId) {
        SupportingRack rack = new SupportingRackBuilder()
                .setRackId(generateRackID(siteLinkId))
                .setGlobalIdentify(globalId)
                .setSupportingNe(new LinkedList<>())
                .setKey(new SupportingRackKey(generateRackID(siteLinkId)))
                .setFriendlyName(rackName)
                .build();

        return rack;
    }

    private Uri generateRackID(String siteLinkId) {
        String id = SiteRackIdNamingRule.generateId(siteLinkId, siteNode.getNodeId().getValue());
        return new Uri(id);
    }

    public SiteNodeCorrelateResource appendTerminationPoint(TpId tpId) {
        ReentrantLock lock = getLock();
        lock.lock();

        try {
            TerminationPointBuilder tpb = new TerminationPointBuilder()
                    .setTpId(tpId)
                    .setKey(new TerminationPointKey(tpId))
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setSite(new SiteBuilder()
                                    .setTpRef(tpId)
                                    .setClientFacing(false)
                                    .setFriendlyName(PhysicalTpIdNamingRule.getPurePortNameByTpId(
                                            tpId.getValue()))
                                    .build())
                            .build()
                    );
            siteNode.getTerminationPoint().add(tpb.build());

            return this;
        } finally {
            lock.unlock();
        }
    }

    public Node getSiteNode() {
        return siteNode;
    }

    public static boolean hasSpaceInRack(Node siteNode, String siteLinkId) {
        Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();
        List<SupportingRack> rackList = siteAttr.getSupportingRack();
        for (SupportingRack rack : rackList) {
            if (rack.getRackId().getValue().contains(siteLinkId)) {
                if (rack.getSupportingNe() != null && rack.getSupportingNe().size() <= 11) {
                    //36， 33， 30， 27， 24， 21， 18， 15， 12， 9， 6， 3, 总共12个位置
                    return true;
                }
            }
        }
        return false;
    }

    private void insert2SiteNode(String siteLinkId, Node phyNode) {
        Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();
        String phyNodeId = phyNode.getNodeId().getValue();

        List<SupportingNode> siteSupportingNodeList = new ArrayList<>(siteNode.getSupportingNode());
        List<SupportingRack> newRackList = new ArrayList<>(siteNodeAttr.getSupportingRack());

        if (! siteNode.getSupportingNode().stream()
                .map(x->x.getNodeRef().getValue())
                .anyMatch(x->x.equals(phyNodeId))) {
            siteSupportingNodeList = insert2SiteNodeSupportingNe(siteSupportingNodeList, phyNodeId);
        }

        boolean existed = false;
        for (SupportingRack rack : siteNodeAttr.getSupportingRack()) {
            if (rack.getSupportingNe().stream()
                    .map(x->x.getNodeRef().getValue())
                    .anyMatch(x->x.equals(phyNodeId))) {
                existed = true;
                break;
            }
        }
        if (!existed) {
            newRackList = insert2Rack(siteLinkId, phyNode);
        }

        rebuildSiteNode(newRackList, siteSupportingNodeList);
    }

    private List<SupportingNode> insert2SiteNodeSupportingNe(List<SupportingNode> siteSupportingNode, String phyNodeId) {
        siteSupportingNode.add(new SupportingNodeBuilder()
                .setNodeRef(new NodeId(phyNodeId))
                .setKey(new SupportingNodeKey(new NodeId(phyNodeId),
                        new TopologyId(TopoNameConstants.Phy_Topo_Key)))
                .build());
        return siteSupportingNode;
    }

    private List<SupportingRack> insert2Rack(String siteLinkId, Node phyNode) {
        Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();
        List<SupportingRack> newRackList = new ArrayList<>(siteNodeAttr.getSupportingRack());

        SupportingRack targetRack;
        targetRack = siteNodeAttr.getSupportingRack().stream()
                .filter(x -> x.getRackId().getValue().contains(siteLinkId))
                .findAny().orElse(null);
        if (targetRack == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("cannot find the siteLink related rack on %s, %s", siteNodeAttr.getFriendlyName(), siteLinkId));
        }

        if (targetRack.getSupportingNe().stream().anyMatch(x -> x.getNodeRef().getValue().equals(phyNode.getNodeId().getValue()))) {
            log.warn("the node has existed in rack");
            return newRackList;
        }

        log.debug("siteNode:  Rack size: will insert on: current neSize: new node \n insert2Rack: {}, {}, {}, {}, {}",
                siteNodeAttr.getFriendlyName(), siteNodeAttr.getSupportingRack().size(),
                targetRack.getFriendlyName(), targetRack.getSupportingNe().size(),
                phyNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical().getFriendlyName());


        List<SupportingNe> newSupportingNe = new ArrayList<>(targetRack.getSupportingNe());
        newSupportingNe.add(new SupportingNeBuilder()
                .setNodeRef(phyNode.getNodeId())
                .setLocation(getLocationInRack(targetRack))
                .setKey(new SupportingNeKey(phyNode.getNodeId()))
                .build());

        String targetRackId = targetRack.getRackId().getValue();
        newRackList.removeIf(x->x.getRackId().getValue().equals(targetRackId));

        targetRack = new SupportingRackBuilder(targetRack).setSupportingNe(newSupportingNe).build();
        newRackList.add(targetRack);

        return newRackList;
    }

    private void rebuildSiteNode(List<SupportingRack> rackList, List<SupportingNode> supportingNode) {
        Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site newSite =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(siteNodeAttr)
                        .setSupportingRack(rackList)
                        .build();

        siteNode = new NodeBuilder(siteNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setSite(newSite)
                        .build())
                .setSupportingNode(supportingNode)
                .build();
    }
}
