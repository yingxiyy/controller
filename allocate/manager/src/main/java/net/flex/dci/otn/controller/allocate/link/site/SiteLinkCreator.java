/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.link.site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.common.util.ocm.LinkRoute;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.common.OpNodeMerger;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.SiteLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import net.flex.dci.otn.controller.allocate.link.phy.AddDropLinkConstructor;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeMerge;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.ProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ComputeLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLink2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ProtectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class SiteLinkCreator<T> {

    private static final Boolean statuschecking = true;
    private static final Pattern MUX_PANEL_MPO_PATTERN =
            Pattern.compile("#MUX-[^#]+#PORT-[^#]+-MPO(\\d+)(?:-|$)");
    protected TaskInfoMessage taskInfoMessage;
//  private TaskInfoKafkaService kafka;

    protected NeDesigner neDesigner;
//  protected SiteLinkDao siteLinkDao;
//  protected TopologyDao topologyDao;
//  protected SiteNodeDao siteNodeDao;

    protected SiteLinkFriendlyName siteLinkFriendlyNameGenerator;
    protected PhyNodeFriendlyName phyNodeFriendlyNameGenerator;
    protected PhyLinkFriendlyName phyLinkFriendlyNameGenerator;

    protected CreateSiteLinkParam param;

    protected RouteInfo routeResource; //计算出来的结果
    protected String siteLinkFriendlyName;
    protected NeYangModel yangModel;
    private NeYangModel configuredYangModel;

    //following attributes are created/updated. should save to DB.
    //============
    protected ChangedObject changedObject;
    protected MultipleTransaction mongoTransaction;
    //============

    //========
    private CreatorMethod_I creationMethod;
    private boolean isRoadm;
    private final AddDropLinkConstructor addDropLinkConstructor = new AddDropLinkConstructor();

    /**
     * calling from network creation
     *
     * @param changedObject
     */
    public SiteLinkCreator(ChangedObject changedObject, boolean isRoadm) {
        init();
        this.changedObject = changedObject;
        this.isRoadm = isRoadm;
    }

    public SiteLinkCreator() {
        init();
        this.isRoadm = false;
    }

    private void init() {
        neDesigner = SpringBeanFinder.getBean(NeDesigner.class);
//     siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
//     topologyDao = SpringBeanFinder.getBean(TopologyDao.class);
//     siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        AllocatorConfig configuration = SpringBeanFinder.getBean(AllocatorConfig.class);
        configuredYangModel = configuration.getYangModel();
        yangModel = configuredYangModel;

        siteLinkFriendlyNameGenerator = SpringBeanFinder.getBean(SiteLinkFriendlyName.class);
        phyNodeFriendlyNameGenerator = SpringBeanFinder.getBean(PhyNodeFriendlyName.class);
        phyLinkFriendlyNameGenerator = SpringBeanFinder.getBean(PhyLinkFriendlyName.class);

        changedObject = new ChangedObject();
    }

    /**
     * 创建SiteLink将修改如下内容 1. siteLink本身 2. siteTopo的riskGroup内容 3.
     * siteNode的supportingNode中添加siteLink涉及的物理网元（phyNode） 4. siteNode的TP点添加siteLink路由中的OTS Link
     * (网元间连接）涉及的TP 5. siteNode的Rack添加， 包含rack中的网元
     * 6. siteLink路由中的phyLink添加supportedLink为此siteLink
     *
     * @param input
     * @return
     * @throws CommonException
     */
    public CreateLinkOutput doIt(T input) throws CommonException {
        log.debug("create siteLink start...");
        //所有的必要参数在checkInput中都提取，且放在类变量中
        parse(input);

        //由于耗时, 把这个同步命令改为异步
        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        if (input instanceof CreateLinkInput) {
            //这个改为同步创建， 主要用于创建 虚拟 siteLink， 为TPC---TPC 的tunnel服务

        }
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                lazy(input);
            }
        });
        CreateLinkOutputBuilder ob = new CreateLinkOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .setReturnMessage("lazy creation start.");
        //    ob.fieldsFrom(createdLink);
        return ob.build();
    }

    private void parse(T input) throws CommonException {
        param = new CreateSiteLinkParam(changedObject);
        if (input instanceof CreateLinkInput) {
            param.parser((CreateLinkInput) input);
            creationMethod = new Method1();
        } else if (input instanceof CreateLink2Input) { //create-network, roadm related
            param.parser((CreateLink2Input) input);
            creationMethod = new Method2(isByteDance2Flex64());
        } else if (input instanceof ComputeLinkInput) {
            param.parser((ComputeLinkInput) input);
            creationMethod = new Method1();
        }
        yangModel = resolveSiteLinkYangModel(configuredYangModel,
                param.getVendorName(), param.getVendorType());
    }

    static NeYangModel resolveSiteLinkYangModel(NeYangModel configuredModel,
                                                 String vendorName,
                                                 String productType) {
        return ProductTypeResolver.isBone20ProductType(vendorName, productType)
                ? NeYangModel.Chassis20 : configuredModel;
    }

    public void lazy(T input) throws CommonException {
        //我调用neDisgner准备数据，同时找到siteLink的起点、终点对应的siteId

        //param 再次生成是保证所有资源都已经锁住
        Link createdLink = creationLogic(input, true);

        mongoTransaction.save(changedObject);

        log.debug("after save to mongo, start merge to OP");
        OpNodeMerger opMerger = new OpNodeMerger();
        changedObject.getChangedPhyNodeList().keySet().forEach(opMerger::merge);
        log.debug("merge to OP done");

        if (input instanceof CreateLink2Input) {
            logMessage(createdLink, null);
        }

        log.debug("create siteLink done.");

    }

    public Link creationLogic(T input, boolean requireLocker) {
        ZkResourceLock locker = new ZkResourceLock();
        try {
            //开始锁资源，一直到createLink写数据库结束。否则提取释放锁会导致生成的ID重复
            if (requireLocker) {
                parse(input);
                lockResource(locker);
            }
            routeResource = getRouteResource(input);

            /**
             * 产生4个修改
             * changedCorrelateNodeList, phyNode的friendlyName， siteNode 与 phyNode 的rack， siteNode 的TP 点
             * risk-group,
             * changedSiteLinkList. created site link
             * changedPhyLinkList (change friendlyName, supportedLink as siteLink)
             */

            Link createdLink = buildTopologyData();
            updateViewLink(createdLink);

            locker.unlock();
            log.debug("unlock {}", siteLinkFriendlyName);

            return createdLink;
        } catch (CommonException ce) {
            if (input instanceof CreateLink2Input) {
                logMessage(null, ce.getMessage());
            }
            log.error("create siteLink error.", ce);
            throw ce;
        } catch (Exception e) {
            log.error("create siteLink error.", e);
            if (input instanceof CreateLink2Input) {
                logMessage(null, e.toString());
            }
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "create siteLink error: " + e.toString(), e);
        } finally {
            log.debug("unlock {}", siteLinkFriendlyName);
            locker.unlock();
        }
    }

    private void updateViewLink(Link createdLink) {
        String siteLinkPlaneId = createdLink.getAugmentation(Link1.class).getSite().getPlaneId();
        ViewLink viewLinkMgr = new ViewLink(this.changedObject, siteLinkPlaneId);
        viewLinkMgr.create(createdLink);

        //related OTS link should adding to viewLink
        List<Link> phyLinks = new ArrayList<>(routeResource.getMain().getLinks());
        if (routeResource.getSlave() != null) {
            phyLinks.addAll(routeResource.getSlave().getLinks());
        }
        if (routeResource.getThird() != null) {
            phyLinks.addAll(routeResource.getThird().getLinks());
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                createdLink.getAugmentation(Link1.class).getSite();

        for (Link link : phyLinks) {
            if (link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical().getLinkType().equals(LinkType.OtsLink)) {

                viewLinkMgr.create(link);
            }
        }
    }

    private void merge2OpDB(Link createdLink) {
        Set<String> nodeIdSet = LinkRoute.getNodesOverSiteLink(createdLink);

        for (String nodeId : nodeIdSet) {
            Node opNode = changedObject.getChangedPhyOpNode(nodeId);
            if (opNode != null) {
                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
                Node newOpNode = new PhyNodeMerge(cfgNode, opNode).add();
                changedObject.addChangedPhyOpNode(newOpNode);
            }
        }
    }

    private void logMessage(Link createdLink, String errorMessage) {
        String msg;
        try {
            String srcFriendlyName = param.getSrcSiteName();
            String dstFriendlyname = param.getDstSiteName();

            msg = String.format("create siteLink between %s, %s ", srcFriendlyName,
                    dstFriendlyname);
        } catch (Exception e) {
            msg = String.format("%s and param parsing error %s", errorMessage, e.getMessage());
        }

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            if (isOk) {
                taskInfoMessage.setResourceId(createdLink.getLinkId().getValue());
                taskInfoMessage.setResourceName(
                        createdLink.getAugmentation(Link1.class).getSite().getFriendlyName());
                taskInfoMessage.setSuccessfully(isOk);
            } else {
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        log.debug(msg + " " + extMsg);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("create siteLink")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }

    /**
     * 只锁siteNode will impact following obj 1. created siteLink  (new obj) 2. phy OPC node      (new
     * obj) 3. related phy link  (new obj) 4. site node         (existed) 5. riskgroup (existed)
     *
     * @param locker
     */
    private void lockResource(ZkResourceLock locker) {
        log.debug("start lock");
        synchronized (statuschecking) {
            for (Node node : param.getSiteNodeMap().values()) {
                locker.addResource(node.getNodeId().getValue());  //防止利旧的时候出错
            }

            locker.getLock();
        }
    }

    /**
     * build siteLink with routeInfo, supportingLink insert the site link into riskGroup update
     * phyNode, phyLink friendlyName update phyLink supportedLink change siteNode insert phyNode
     * into siteNode's Rack (one OPC one Rack) append this phyNode into siteNode's supporting-node
     * append the siteLink a/z TP into siteNode's TP 参数 routeResource 是类变量 输出 createdSiteLink,
     * changedPhyLink, changedSiteNode+phyNode (correlatedNode), riskGroup
     */
    private Link buildTopologyData() {
        log.debug("build site link related topo data start...");

        Link createdLink = buildSiteLink();

        updateRiskGroup(createdLink);
        changedObject.addChangedSiteLink(createdLink);

        updatePhysicalResource(createdLink, routeResource.getMain(), param.getMainSegment());

        if (routeResource.getSlave() != null) {
            updatePhysicalResource(createdLink, routeResource.getSlave(), param.getSlaveSegment());

            if (routeResource.getThird() != null) {
                updatePhysicalResource(createdLink, routeResource.getThird(),
                        param.getSpareSegment());
            }
        }

        updateSiteNodeType();
        updatePhyResourceForReuse(createdLink);

//    Node srcNode = routeResource.getMain().getNodes().get(0);
//    Node dstNode = routeResource.getMain().getNodes().get(routeResource.getMain().getNodes().size() - 1);
//    String siteLinkFriendlyName = siteLinkFriendlyNameGenerator.buildFriendlyName(param, srcNode, dstNode);
//    Link updatedLink = new LinkBuilder(createdLink)
//            .addAugmentation(Link1.class, new Link1Builder()
//                    .setSite(new SiteBuilder(createdLink.getAugmentation(Link1.class).getSite())
//                            .setFriendlyName(siteLinkFriendlyName)
//                            .build())
//                    .build())
//            .build();
        log.debug("build site link related topo data done");
        return createdLink;
    }

    private void updateSiteNodeType() {
        updateSiteNodeType(param.getSegments());
        updateSiteNodeType(param.getMainSegment());
        updateSiteNodeType(param.getSpareSegment());
        updateSiteNodeType(param.getSlaveSegment());
    }

    private void updateSiteNodeType(List<Segment> segments) {
        if (segments == null) {
            return;
        }
        segments.forEach(seg -> {
            String srcId = seg.getSource();
            LinkTerminationNodeType srcType = seg.getSourceNodeType();
            updateSiteNodeType(srcId, convert2SiteType(srcType));

            String dstId = seg.getDestination();
            LinkTerminationNodeType dstType = seg.getDestinationNodeType();
            updateSiteNodeType(dstId, convert2SiteType(dstType));
        });
    }

    private SiteType convert2SiteType(LinkTerminationNodeType nodeType) {
        switch (nodeType) {
            case SITE:
                return SiteType.SITE;
            case ILA:
                return SiteType.ILA;
            case DGE:
                return SiteType.DGE;
            case ROADM:
                return SiteType.ROADM;
            case OTM:
                return SiteType.OTM;
            case REG:
                return SiteType.OTM;
        }
        return SiteType.SITE;
    }

    private void updateSiteNodeType(String nodeId, SiteType nodeType) {
        Node node = changedObject.getChangedSiteNode(nodeId);
        if (node == null) {
            return;
        }
        Site nodeAttr = node.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        SiteType now = nodeAttr.getSiteType();
        if (nodeType.compareTo(now) > 0) {
            Node newNode = new NodeBuilder(node).addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                            new Node1Builder().setSite(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(
                                                    nodeAttr)
                                                    .setSiteType(nodeType)
                                                    .build())
                                    .build())
                    .build();

            changedObject.addChangedSiteNode(newNode);
        }
    }


    private void updatePhysicalResource(Link createdLink,
            net.flex.dci.otn.controller.allocate.designer.model.Route route,
            List<Segment> segments) {
        List<Node> newPhyNodeList = updatePhyNode(route.getNodes());
        List<Node> newSiteNodeList = updateSiteNodeResource(newPhyNodeList, createdLink);

        newPhyNodeList = updatePhyNodeFriendlyName(newPhyNodeList, newSiteNodeList);
        List<Link> newPhyLinkList = updatePhyLinkFriendlyName(route.getLinks());
        updatePhyLink(newPhyLinkList, createdLink, segments);
    }

    /**
     * 新功能需求，如果板卡用于某条线，在这条线删除后，这个板卡应该清空（EMPTY）， 但是
     * 如果此网元有IP地址，OP树上反应此位置有真实板卡，那么这个属性仅仅清0（flase)，而不是把板卡设置为EMPTY
     *
     * 此函数设置板卡被占用
     *
     * @param createdLink
     */
    private void updatePhyResourceForReuse(Link createdLink) {
        Map<String, List<String>> nodeEqMap = LinkRoute.getEquipOverSiteLink(createdLink);
        for (String nodeId : nodeEqMap.keySet()) {
            Node cfgNode = changedObject.getChangedPhyNode(nodeId);
            Node newCfgNode = PhyNodeUtil.addEquipUsedSymbol(cfgNode, nodeEqMap.get(nodeId), true);
            changedObject.addChangedPhyNode(newCfgNode);

//      Node opNode = changedObject.getChangedPhyOpNode(nodeId);
//      if (opNode != null) {
//        Node newOpNode = PhyNodeUtil.addEquipUsedSymbol(cfgNode, nodeEqMap.get(nodeId), true);
//        changedObject.addChangedPhyOpNode(newOpNode);
//      }
        }
    }

    private List<Link> updatePhyLinkFriendlyName(List<Link> links) {
        List<Link> newLinks = new ArrayList<>();
        PhyLinkFriendlyName phyLinkFriendlyNameGenerator = SpringBeanFinder.getBean(
                PhyLinkFriendlyName.class);

        for (Link phyLink : links) {
            Node srcNode = changedObject.getChangedPhyNode(
                    phyLink.getSource().getSourceNode().getValue());
            Node dstNode = changedObject.getChangedPhyNode(
                    phyLink.getDestination().getDestNode().getValue());
            Link newLink = phyLinkFriendlyNameGenerator.updateFriendlyName(phyLink, srcNode,
                    dstNode);
            changedObject.addChangedPhyLink(newLink);
            newLinks.add(newLink);
        }

        return newLinks;
    }

    private List<Node> updatePhyNode(List<Node> phyNodeList) {
        List<Node> newNodes = new ArrayList<>();

        for (Node phyNode : phyNodeList) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical oldPhyAttr =
                    phyNode.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                            .getPhysical();

            List<String> orderId = new LinkedList<>();
            orderId.add(param.getOrderId());
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical newPhyAttr =
                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                            oldPhyAttr)
                            .setRiskGroupName(param.getRiskGroupName())
                            .setPlaneName(param.getPlaneName())
                            .setPlaneId(param.getPlaneId())
                            .setOrderId(orderId)
                            .setProperties(
                                    updateNodeYangModel(oldPhyAttr.getProperties()))
                            .build();

            newNodes.add(new NodeBuilder(phyNode)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder()
                                    .setPhysical(newPhyAttr).build())
                    .build());
        }
        return newNodes;
    }

    private List<Node> updatePhyNodeFriendlyName(List<Node> phyNodeList, List<Node> siteNodeList) {
        List<Node> newNodes = new ArrayList<>();
        PhyNodeFriendlyName phyNodeFriendlyNameGenerator = SpringBeanFinder.getBean(
                PhyNodeFriendlyName.class);

        for (Node phyNode : phyNodeList) {
            for (Node siteNode : siteNodeList) {
                if (PhysicalNodeIdNamingRule.getSiteId(phyNode.getNodeId().getValue())
                        .equals(siteNode.getNodeId().getValue())) {
                    Node newNode = phyNodeFriendlyNameGenerator.updateFriendlyName(phyNode,
                            siteNode);
                    changedObject.addChangedPhyNode(newNode);
                    newNodes.add(newNode);
                    break;
                }
            }
        }
        return newNodes;
    }

    /**
     * 添加物理连接支撑对象（siteLink),
     *
     * @param links
     * @param createdSiteLink
     * @param segments
     * @return
     */
    private List<Link> updatePhyLink(List<Link> links, Link createdSiteLink,
            List<Segment> segments) {
        log.debug("update physical link start...");
        List<Link> newLinks = new LinkedList<>();
        for (Link link : links) {
            Physical oldPhyAttr = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical();
//      if (oldPhyAttr.getLinkType().equals(LinkType.CableLink))
//        continue;

            List<SupportedLink> supportedLinkList = new LinkedList<>();
            supportedLinkList.add(new SupportedLinkBuilder()
                    .setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key))
                    .setLinkRef(createdSiteLink.getLinkId())
                    .build());

            List<String> orderId = new LinkedList<>();
            orderId.add(param.getOrderId());

            PhysicalBuilder newPhyAttr = new PhysicalBuilder(oldPhyAttr)
                    .setRiskGroupName(param.getRiskGroupName())
                    .setPlaneName(param.getPlaneName())
                    .setPlaneId(param.getPlaneId())
                    .setOrderId(orderId)
                    .setSupportedLink(supportedLinkList);
            if (oldPhyAttr.getLinkType().equals(LinkType.OtsLink)) {
                newPhyAttr.setProvider(getProvider(link, segments));

                Properties newPro = PropertyTool.addProperty(newPhyAttr.getProperties(),
                        "sourceSiteNodeType",
                        param.getSiteTypeInLink(PhysicalTpIdNamingRule
                                .getSiteId(link.getSource().getSourceTp().getValue())).name());

                newPro = PropertyTool.addProperty(newPro,
                        "destinationSiteNodeType",
                        param.getSiteTypeInLink(PhysicalTpIdNamingRule.getSiteId(
                                link.getDestination().getDestTp().getValue())).name());

                newPhyAttr.setProperties(new PropertiesBuilder(newPro).build());
                log.debug("update OTS phylink {} and properties are {}", link.getLinkId().getValue(), newPro);
            }

            Link newLink = new LinkBuilder(link).addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                    .setPhysical(newPhyAttr.build())
                                    .build())
                    .build();
            changedObject.addChangedPhyLink(newLink);
            newLinks.add(newLink);
        }
        log.debug("update physical link done.");
        return newLinks;
    }

    //this is special for OTS link
    private Provider getProvider(Link link, List<Segment> segments) {
        //find link from segment
        String srcSite = PhysicalNodeIdNamingRule.getSiteId(
                link.getSource().getSourceNode().getValue());
        String dstSite = PhysicalNodeIdNamingRule.getSiteId(
                link.getDestination().getDestNode().getValue());
        for (Segment seg : segments) {
            if (srcSite.equals(seg.getSource()) && dstSite.equals(seg.getDestination())) {
                return updateDistanceDelayInfo(seg.getProvider(), true);
            } else if (srcSite.equals(seg.getDestination()) && dstSite.equals(seg.getSource())) {
                return updateDistanceDelayInfo(seg.getProvider(), false);
            }
        }
        return null;
    }

    private Provider updateDistanceDelayInfo(Provider original, boolean directionAligned) {
        try {
            ProviderBuilder provideB = new ProviderBuilder(original);
            if (original.getDistanceAz() == null) {
                if (directionAligned) {
                    provideB.setDistanceAz(original.getDistance());
                } else {
                    provideB.setDistanceZa(original.getDistance());
                }
            }
            if (original.getDistanceZa() == null) {
                if (directionAligned) {
                    provideB.setDistanceZa(original.getDistance());
                } else {
                    provideB.setDistanceAz(original.getDistance());
                }
            }

            if (original.getAttenuationAz() == null) {
                if (directionAligned) {
                    provideB.setAttenuationAz(original.getAttenuation());
                } else {
                    provideB.setAttenuationZa(original.getAttenuation());
                }
            }
            if (original.getAttenuationZa() == null) {
                if (directionAligned) {
                    provideB.setContractAttenuationZa(original.getAttenuation());
                } else {
                    provideB.setContractAttenuationAz(original.getAttenuation());
                }
            }

            if (original.getContractAttenuationAz() == null) {
                if (directionAligned) {
                    provideB.setContractAttenuationAz(original.getAttenuationAz());
                } else {
                    provideB.setContractAttenuationZa(original.getAttenuationAz());
                }
            }
            if (original.getContractAttenuationZa() == null) {
                if (directionAligned) {
                    provideB.setContractAttenuationZa(original.getAttenuationZa());
                } else {
                    provideB.setContractAttenuationAz(original.getAttenuationZa());
                }
            }

            if (directionAligned) {
                provideB.setDelayAz(calculateDelay(provideB.getDistanceAz()));
                provideB.setDelayZa(calculateDelay(provideB.getDistanceZa()));
            } else {
                provideB.setDelayZa(calculateDelay(provideB.getDistanceAz()));
                provideB.setDelayAz(calculateDelay(provideB.getDistanceZa()));
            }
            return provideB.build();
        } catch (Exception e) {
            log.error("error happen ", e);
            throw e;
        }
    }

    //在光纤中，由于玻璃材料的折射率（通常是 ~1.5），光速被减慢了， 所以我们常用 200,000 km/s 来计算光纤中的延迟。
    private BigDecimal calculateDelay(BigDecimal distanceKm) {
        BigDecimal delay = distanceKm.divide(BigDecimal.valueOf(200), 10, RoundingMode.HALF_UP);
        return delay.setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places
    }

    private void updateRiskGroup(Link createdSiteLink) {
        changedObject.changRiskGroup_add(param.getRiskGroupName(), param.getPlaneName(),
                createdSiteLink.getLinkId());
    }


    /**
     * 要求routeResource中的nodes, links 是顺序排列的 0                   1 size-2                size-1
     * siteNodeA              ILA                                  ILA siteNodeZ
     *
     *
     * 0                   1                                 size-2                size-1 cableLink
     * siteLinkStartPointRelatedLink ........siteLinkEndPointRelatedLink  cableLink
     *
     * 这里的cableLink是指无源MUX板卡上到OPC网元的Panel的供电连接，便于读出MUX板卡上的EPOM信息（厂家，板卡型号等）
     * 第一个node=link.srcNode==>siteLink的起点是link.srcTp 同理 最后一个node。。。。 参数 routeResource 是类变量
     */
    private Link buildSiteLink() throws CommonException {
        log.debug("build site link start...");

        Source srcTp = covert2SiteSrcTp(creationMethod.getLinkSrcTermination(routeResource));
        Destination dstTp = covert2SiteDstTp(creationMethod.getLinkDstTermination(routeResource));

        String siteLinkId = SiteLinkIdNamingRule.generateId(srcTp.getSourceTp().getValue(),
                dstTp.getDestTp().getValue());
        //UI 数据重复发送导致已经创建的siteLink被要求再次创建
        Link existedSiteLink = changedObject.getChangedSiteLink(siteLinkId);
        if (existedSiteLink != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "BOM required siteLink has created");
        }

        LinkId linkId = new LinkId(siteLinkId);
        Link createdLink = new LinkBuilder().setLinkId(linkId)
                .setSource(srcTp)
                .setDestination(dstTp)
                .setKey(new LinkKey(linkId))
                .setSupportingLink(getSupportingLink())
                .addAugmentation(Link1.class, getSiteLinkArgument(srcTp, dstTp))
                .build();
        log.debug("build site link done.");
        return createdLink;
    }


    private Destination covert2SiteDstTp(Destination dstTp) {
        Destination dst = new DestinationBuilder()
                .setDestNode(new NodeId(
                        PhysicalNodeIdNamingRule.getSiteId(dstTp.getDestNode().getValue())))
                .setDestTp(convertMPOTP(dstTp.getDestTp()))
                .build();

        return dst;
    }

    private TpId convertMPOTP(TpId phyTpId) {
        String[] tmp = phyTpId.getValue().split("MPO");
        if (tmp.length > 1) {
            return new TpId(tmp[0] + "MPO");
        }
        return phyTpId;
    }

    private Source covert2SiteSrcTp(Source srcTp) {
        Source src = new SourceBuilder()
                .setSourceNode(new NodeId(
                        PhysicalNodeIdNamingRule.getSiteId(srcTp.getSourceNode().getValue())))
                .setSourceTp(convertMPOTP(srcTp.getSourceTp()))
                .build();

        return src;
    }

    private Node getSiteNode(Node phyNode) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(phyNode.getNodeId().getValue());
        return changedObject.getChangedSiteNode(siteNodeId);
    }

    private Link1 getSiteLinkArgument(Source srcTp, Destination dstTp) {
        siteLinkFriendlyName = siteLinkFriendlyNameGenerator.buildFriendlyName(
                changedObject.getChangedSiteNode(srcTp.getSourceNode().getValue()),
                changedObject.getChangedSiteNode(dstTp.getDestNode().getValue()),
                param.getRiskGroupName(), param.getPlaneName());

        Route route = new Route(routeResource, Route.RouteType.SiteLink).setRoadm();
        Link1Builder lb = new Link1Builder().setSite(new SiteBuilder()
                .setBandwidth(getBandwidth())
                // link-model is the source of truth for OLP3-3 capability; route roles only say which legs exist.
                .setProtectionType(param.getProtectionType())
                .setOrderId(getOrderId())
                .setAdminState(AdminStatus.Unknown)
                .setProperties(getSiteLinkProperties())
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setPlaneName(param.getPlaneName())
                .setPlaneId(param.getPlaneId())
                .setGrid(param.getGrid())
                .setProtectionInfo(
                        new ProtectionInfoBuilder().setWorkingRole(ProtectionRole.Main).build())
                .setFriendlyName(siteLinkFriendlyName)
                .setImplementState(ImplementState.Allocate)
                .setCreationTime(param.getCreationTime())
                .setOperationalState(OperStatus.Unknown)
                .setAvailable(FrequencyAvailable.getInitializedAvailableList(yangModel,
                        param.getLinkGroup(), param.getGrid()))
                .setExplictRoute(route.getExplictRoute(srcTp.getSourceTp().getValue(),
                        dstTp.getDestTp().getValue()))
                .setSingleFrequencyPower(new BigDecimal(param.DEFAULT_OT_LINE_TX_POWER))
                .setAlarmState(AlarmSeverity.Unknown)
                .setRiskGroupName(param.getRiskGroupName())
                .setSupportedLink(new LinkedList<>())
                .setVendorName(param.getVendorName())

                .setProductType(param.getVendorType())
                .setLinkGroup(param.getLinkGroup().toString())
                .setAExternal(getAExternal(routeResource.getMain(), srcTp))
                .setZExternal(getZExternal(routeResource.getMain(), dstTp))
                .setAType(route.getaType())
                .setZType(route.getzType())

                .build());

        return lb.build();
    }

    private AExternal getAExternal(net.flex.dci.otn.controller.allocate.designer.model.Route main,
                                   Source srcTp) {
        Node endpoint = isByteDance2FlexProtected()
                ? findEndpointNode(main.getNodes(), srcTp.getSourceNode().getValue())
                : main.getNodes().get(0);
        return new AExternalBuilder().setAddDropLink(getAddDropLinks(main, endpoint)).build();
    }

    private ZExternal getZExternal(net.flex.dci.otn.controller.allocate.designer.model.Route main,
                                   Destination dstTp) {
        Node endpoint = isByteDance2FlexProtected()
                ? findEndpointNode(main.getNodes(), dstTp.getDestNode().getValue())
                : main.getNodes().get(main.getNodes().size() - 1);
        return new ZExternalBuilder().setAddDropLink(getAddDropLinks(main, endpoint)).build();
    }

    private List<AddDropLink> getAddDropLinks(
            net.flex.dci.otn.controller.allocate.designer.model.Route main, Node node) {
        List<AddDropLink> wssLinks = getLink2WSS(node, main.getLinks());
        List<AddDropLink> extLinks = getLinkBetweenMuxPanelCmux(node, main.getLinks());

        extLinks.addAll(wssLinks);
        if (isByteDance2Flex32Or64()) {
            // Bone2.0 Flex follows C+L external semantics: FMUX internal phy links are not add/drop links.
            List<AddDropLink> externalLinks = extLinks.stream()
                    .filter(link -> !isFmux32InternalAddDrop(link))
                    .collect(Collectors.toList());
            if (isByteDance2Flex64()) {
                return validateAndSortFlex64ExternalLinks(externalLinks);
            }
            return externalLinks;
        }
        return extLinks;
    }

    static Node findEndpointNode(List<Node> nodes, String siteId) {
        for (Node node : nodes) {
            if (siteId.equals(PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue()))) {
                return node;
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Cannot find physical endpoint node for site " + siteId);
    }

    private List<AddDropLink> validateAndSortFlex64ExternalLinks(List<AddDropLink> externalLinks) {
        Map<String, AddDropLink> uniqueLinks = new LinkedHashMap<>();
        externalLinks.forEach(link -> uniqueLinks.put(link.getLinkRef(), link));
        List<AddDropLink> sortedLinks = new ArrayList<>(uniqueLinks.values());
        sortedLinks.sort(Comparator
                .comparingInt((AddDropLink link) -> getMuxPanelMpoNumber(link.getLinkRef()))
                .thenComparing(AddDropLink::getLinkRef));

        List<Integer> mpoNumbers = sortedLinks.stream()
                .map(link -> getMuxPanelMpoNumber(link.getLinkRef()))
                .distinct()
                .collect(Collectors.toList());
        if (sortedLinks.size() != 8
                || !mpoNumbers.equals(java.util.Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8))) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Bone2.0 Flex64 requires exactly MUXPANEL MPO1~8 external links, but got "
                            + mpoNumbers);
        }
        return sortedLinks;
    }

    static int getMuxPanelMpoNumber(String linkRef) {
        Matcher matcher = MUX_PANEL_MPO_PATTERN.matcher(String.valueOf(linkRef));
        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private boolean isByteDance2Flex64() {
        return isByteDance2Flex64(param.getVendorName(), param.getVendorType(),
                param.getGrid().getIntValue(), param.getLinkGroup(), param.getBandwidth(),
                param.getProtectionType());
    }

    static boolean isByteDance2Flex64(String vendorName, String vendorType, int grid,
            WDM_Band linkGroup, Integer bandwidth,
            Class<? extends ProtectionType> protectionType) {
        return ProductTypeResolver.isBone20ProductType(vendorName, vendorType)
                && grid == 0
                && linkGroup == WDM_Band.C
                && Integer.valueOf(64).equals(bandwidth)
                && (ProtectionUnprotected.class.equals(protectionType)
                || ProtectionBidir1To1.class.equals(protectionType)
                || ProtectionBidir1To2.class.equals(protectionType));
    }

    private boolean isByteDance2FlexProtected() {
        return isByteDance2FlexProtected(param.getVendorName(), param.getVendorType(),
                param.getGrid().getIntValue(), param.getLinkGroup(), param.getBandwidth(),
                param.getProtectionType());
    }

    static boolean isByteDance2FlexProtected(String vendorName, String vendorType, int grid,
            WDM_Band linkGroup, Integer bandwidth,
            Class<? extends ProtectionType> protectionType) {
        return ProductTypeResolver.isBone20ProductType(vendorName, vendorType)
                && grid == 0
                && linkGroup == WDM_Band.C
                && (Integer.valueOf(32).equals(bandwidth)
                || Integer.valueOf(64).equals(bandwidth))
                && (ProtectionBidir1To1.class.equals(protectionType)
                || ProtectionBidir1To2.class.equals(protectionType));
    }

    private boolean isByteDance2Flex32Or64() {
        return isBone20()
                && param.getGrid().getIntValue() == 0
                && (Integer.valueOf(32).equals(param.getBandwidth()) || Integer.valueOf(64).equals(param.getBandwidth()));
    }

    private boolean isBone20() {
        return ProductTypeResolver.isBone20ProductType(param.getVendorName(), param.getVendorType());
    }

    private boolean isFmux32InternalAddDrop(AddDropLink link) {
        String linkRef = link.getLinkRef();
        return isFmux32TilaAddDrop(linkRef) || isFmux32SigComAddDrop(linkRef);
    }

    private boolean isFmux32TilaAddDrop(String linkRef) {
        return linkRef != null && linkRef.contains("FMUX_32") && linkRef.contains("TILA");
    }

    private boolean isFmux32SigComAddDrop(String linkRef) {
        return linkRef != null && containsPort(linkRef, "COM1") && containsPort(linkRef, "SIG");
    }

    private boolean containsPort(String linkRef, String portName) {
        return linkRef.contains("#PORT-") && linkRef.matches(".*#PORT-[^#-]+-[^#-]+-" + portName + "($|-Site-).*");
    }

    private List<AddDropLink> getLinkBetweenMuxPanelCmux(Node node, List<Link> links) {
        return addDropLinkConstructor.getLinkBetweenMuxPanelCmux(node, links);
    }

    /**
     * 复用段得起止点是WSS/IRA 的Line 口
     *
     * @param node
     * @param links
     * @return
     */
    public List<AddDropLink> getLink2WSS(Node node, List<Link> links) {
        return addDropLinkConstructor.getLink2WSS(node, links);
    }

    private List<String> getOrderId() {
        List<String> rst = new LinkedList<>();
        rst.add(param.getOrderId());
        return rst;
    }

    private Properties getSiteLinkProperties() {
        List<Property> pros = new ArrayList<>();
        Property pro = new PropertyBuilder()
                .setName("model")
                .setName("model")       //protection model
                .setValue(param.getModelType())  //2/4/6 normal, omstp, otsp
                .build();
        pros.add(pro);

        pro = new PropertyBuilder()
                .setName("yang-model")
                .setName("yang-model")
                .setValue(yangModel.name())
                .build();
        pros.add(pro);

        if (isByteDance2Flex32Or64()) {
            // Only Bone2.0 Flex owns the selected 32/64-wave model; fixed grids keep the 2606 grid capacity.
            pro = new PropertyBuilder()
                    .setName("resource-bandwidth")
                    .setValue(getBandwidth())
                    .build();
            pros.add(pro);
        }

        pro = new PropertyBuilder()
                .setName("in-network")
                .setName("in-network")
                .setValue(isRoadm ? "true" : "false")
                .build();
        pros.add(pro);

        return new PropertiesBuilder().setProperty(pros).build();
    }

    private Properties updateNodeYangModel(Properties existed) {
        // Bone2.0 nodes can originate from a ByteDance template, so their persisted
        // model must be overwritten. All other products retain the original add-only rule.
        return NeYangModel.Chassis20.equals(yangModel)
                ? PropertyTool.addProperty(existed, "yang-model", yangModel.name())
                : insertProperties(existed, "yang-model", yangModel.name());
    }

    private Properties insertProperties(Properties existed, String key, String value) {
        List<Property> pros;
        if (existed != null) {
            pros = existed.getProperty();
        } else {
            pros = new ArrayList<>();
        }
        if (!pros.stream().filter(x -> x.getName().toLowerCase().equals(key)).findFirst()
                .isPresent()) {
            Property pro = new PropertyBuilder()
                    .setKey(new PropertyKey(key))
                    .setName(key)
                    .setValue(value)
                    .build();
            pros.add(pro);
        }

        return new PropertiesBuilder().setProperty(pros).build();
    }

    /**
     * export the siteLink support how many och link (frequency)
     *
     * @return
     */
    private String getBandwidth() {
        // Only Bone2.0 Flex owns the UI-selected capacity; all fixed-grid products keep the 2606 grid-based bandwidth.
        if (isByteDance2Flex32Or64() && param.getBandwidth() != null) {
            return String.valueOf(param.getBandwidth());
        }
        switch (param.getGrid()) {
            case _0:
                //this is flex, currently supported frequency number is 64
                return "64";
            case _50:
                return "96";
            case _75:
                return "64";
            case _100:
                return "48";
            case _150:
                return "64";
        }

        return "0";
    }

    /**
     * routeResource中的所有Link 作为siteLink的支撑层
     *
     * @param
     * @return
     */
    private List<SupportingLink> getSupportingLink() {
        List<SupportingLink> slist = new LinkedList<>();
        for (Link link : routeResource.getMain().getLinks()) {
//            Physical phyLinkAttr = link.getAugmentation(
//                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
//                    .getPhysical();
//      if (phyLinkAttr.getLinkType().equals(LinkType.CableLink))
//        continue;

            slist.add(new SupportingLinkBuilder()
                    .setLinkRef(link.getLinkId())
                    .setKey(new SupportingLinkKey(link.getLinkId()))
                    .build());
        }
        if (routeResource.getSlave() != null) {
            for (Link link : routeResource.getSlave().getLinks()) {
                slist.add(new SupportingLinkBuilder()
                        .setLinkRef(link.getLinkId())
                        .setKey(new SupportingLinkKey(link.getLinkId()))
                        .build());
            }
        }
        if (routeResource.getThird() != null) {
            for (Link link : routeResource.getThird().getLinks()) {
                slist.add(new SupportingLinkBuilder()
                        .setLinkRef(link.getLinkId())
                        .setKey(new SupportingLinkKey(link.getLinkId()))
                        .build());
            }
        }
        return slist;
    }


    /**
     * 1. siteNode 添加新rack 2. siteNode 中添加新phyNode到supporting-node 3. siteNode 的TP中添加新TP （OTS link
     * TP)
     *
     * 因为每个OPC网元一个机架，所以直接放进去，机架 每一个phyNode都在rack的顶端，创建好后放入site的rackList rack 的friendly
     * Name就是siteLink的friendly Name
     *
     * @param opcNodes
     * @param siteLink
     */
    private List<Node> updateSiteNodeResource(List<Node> opcNodes, Link siteLink) {
        List<Node> newSiteNodeList = new ArrayList<>();
        for (Node phyNode : opcNodes) {
            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(phyNode.getNodeId().getValue());
            Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
            SiteNodeCorrelateResource correlateResource = creationMethod.createSiteNodeCorrelateResource(
                    siteNode, siteLink, phyNode);

            //只有端站有TP点的更新需求
            if (siteNode.getNodeId().equals(siteLink.getSource().getSourceNode())) {
                correlateResource.appendTerminationPoint(siteLink.getSource().getSourceTp());
            } else if (siteNode.getNodeId().equals(siteLink.getDestination().getDestNode())) {
                correlateResource.appendTerminationPoint(siteLink.getDestination().getDestTp());
            }
            changedObject.addChangedSiteNode(correlateResource.getSiteNode());
            newSiteNodeList.add(correlateResource.getSiteNode());
        }
        return newSiteNodeList;
    }

    /**
     * 根据输入的segment list
     *
     * @param input
     * @return
     * @throws CommonException
     */
    private RouteInfo getRouteResource(T input) throws CommonException {
        log.debug("build site link route info start...");

        RouteInfo info = null;
        try {
            info = creationMethod.allocateResource(neDesigner, input, param);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neDesigner error" + e.getCause().getMessage(), e);
        }
        log.debug("build site link route info done. {}");
        return info;
    }


    public SiteLinkCreator<T> setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
