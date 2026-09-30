/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.site;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.SiteNodeIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.TopologyInitialor;
import net.flex.dci.otn.controller.allocate.common.namingrule.SiteNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.node.view.graph.GraphHelp;
import net.flex.dci.otn.controller.allocate.node.view.ViewNode;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import net.flex.dci.otn.db.jpa.service.dao.SiteInfoDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateSitesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateSitesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateSitesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.create.sites.input.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.springframework.beans.BeansException;


/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
public class SiteNodeCreator {

    protected ChangedObject changedObject;
    //    private TaskInfoKafkaService kafka;
    protected MultipleTransaction mongoTransaction;
    private TaskInfoMessage taskInfoMessage;
    private TopologyInitialor initialor;
    private SiteNodeFriendlyName siteNodeFriendlyName;

    public SiteNodeCreator() {
        init();
    }

    private void init() {
        siteNodeFriendlyName = SpringBeanFinder.getBean(SiteNodeFriendlyName.class);
        initialor = SpringBeanFinder.getBean(TopologyInitialor.class);
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);

        changedObject = new ChangedObject();
    }

    public CreateSitesOutput doIt(CreateSitesInput input) throws CommonException {
        log.debug("start create site node ");
        CreateSitesOutputBuilder outputBuilder = new CreateSitesOutputBuilder();

        List<Site> siteList = input.getSite();
        check(siteList);

        ZkResourceLock allocateLocker = new ZkResourceLock();
        try {
            allocateLocker.addResource(TopoNameConstants.Site_Topo_Key);
            allocateLocker.getLock();
            initialor.initTopo();

            for (Site site : siteList) {
                String nodeId = SiteNodeIdNamingRule.getNewSiteId();
                Node siteNode = convertYangModelNodeForCreate(site, nodeId);
                changedObject.addChangedSiteNode(siteNode);

                if (!siteNode.getAugmentation(Node1.class).getSite().isIsVirtual()) {
                    Node viewNode = new ViewNode(siteNode.getNodeId())
                            .create(siteNode.getAugmentation(Node1.class).getSite()
                                    .getFriendlyName());

                    changedObject.addChangedViewNode(viewNode);
                }
                TimeUnit.MILLISECONDS.sleep(100);
            }

            if (siteList.size() > 5) {
                new GraphHelp(changedObject).layout(changedObject.getChangedViewNodeList());
            }

            mongoTransaction.save(changedObject);
            logMessage(changedObject.getChangedSiteNodeList().values(), null);

            outputBuilder.setReturnCode(RpcResultType.Success);
            log.debug("create site node done ");
        } catch (CommonException ce) {
            logMessage(null, ce.getMessage());
            log.error("create site node fail ", ce);
            throw ce;
        } catch (Exception e) {
            logMessage(null, e.toString());
            log.error("create site node fail ", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.toString(), e);
        } finally {
            allocateLocker.unlock();
        }

        return outputBuilder.build();
    }

    private void logMessage(Collection<Node> createdSiteNodeList, String errorMessage) {
        String msg = "create siteNodes";

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            if (isOk) {
                if (createdSiteNodeList.size() > 1) {//send a summarize
                    taskInfoMessage.setResourceId(msg);
                    taskInfoMessage.setResourceName("siteNodes");
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(null);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                    taskInfoMessage.setDetail(null);  //存储单个siteNode的时候detail不需要了
                }

                for (Node node : createdSiteNodeList) {
                    taskInfoMessage.setResourceId(node.getNodeId().getValue());
                    taskInfoMessage.setResourceName(
                            node.getAugmentation(Node1.class).getSite().getFriendlyName());
                    taskInfoMessage.setSuccessfully(isOk);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                }
            } else {
                taskInfoMessage.setResourceId(msg);
                taskInfoMessage.setResourceName("siteNodes");
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);

                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }
    }


    /**
     * 检查siteTopo数据是否存在，不存在创建
     */
    private void initTopo() {

    }

    private void check(List<Site> siteList) throws CommonException {
        if (siteList == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site List is empty");
        }

        List<Site> newSiteList = new ArrayList<>();
        for (Site site : siteList) {
            Site newSite = parseSite(site);
            try {
                siteNodeFriendlyName.checkFridendlyName(newSite.getSite().getFriendlyName());
                newSiteList.add(newSite);
            } catch (CommonException e) {
                log.error("invalid site info :{}", e.getMessage(), e);
                throw e;
            }
        }
        siteList.clear();
        siteList.addAll(newSiteList);
    }

    private Site parseSite(Site site) throws CommonException {
        if (site.getSite() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "hasn't provide site detail info");
        }

        SiteInfoDaoService siteInfoDao = SpringBeanFinder.getBean(SiteInfoDaoService.class);
        if (site.getSite().getProperties() != null && !site.getSite().getProperties().getProperty()
                .isEmpty()) {
            List<Property> propList = site.getSite().getProperties().getProperty();
            for (Property prop : propList) {
                if (prop.getName().equalsIgnoreCase("siteInfoId")) {
                    long siteInfoId = Long.valueOf(prop.getValue());
                    try {
                        SiteInfo info = siteInfoDao.findOne(siteInfoId);
                        if (info == null || info.getId() == null) {
                            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                    "cannot find the IDC record.");
                        }
                        Site newSite = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.create.sites.input.SiteBuilder()
                                .setSite(new SiteBuilder()
//                            .setRoom(info.getRoom())
                                        .setCampus(info.getCampus())  //园区名称
                                        .setFriendlyName(info.getRoom().trim())  //机房名称
                                        .setCity(info.getCity())
                                        .setCode(info.getRoomCode())
                                        .setRegion(info.getRegion())
                                        .setDomainName(info.getRegion())
                                        .setProperties(site.getSite().getProperties())
                                        .build())
                                .build();
                        return newSite;
                    } catch (BeansException e) {
                        log.error("DB reading error, cannot found SiteInfoDaoService");
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                "cannot find the IDC record.");
                    }
                }
            }
        }
        return site;
    }


    private Node convertYangModelNodeForCreate(Site site, String nodeId) {
        String friendlyName = site.getSite().getFriendlyName();
        String code = "tmp", region = null, city = null, campus = null, domain = null, room = null;

        if (site.getSite().getDomainName() != null) {
            domain = site.getSite().getDomainName();
        }
        if (site.getSite().getRegion() != null) {
            region = site.getSite().getRegion();
        }
        if (site.getSite().getCity() != null) {
            city = site.getSite().getCity();
        }
        if (site.getSite().getCampus() != null) {
            campus = site.getSite().getCampus();
        }

        if (site.getSite().getCode() != null) {
            code = site.getSite().getCode();
        }
//    if (site.getSite().getRoom() != null)
//      room = site.getSite().getRoom();
//    else
//      room = site.getSite().getCampus();

        SiteBuilder siteBuilder = new SiteBuilder()
                .setAdminState(AdminStatus.Unknown)
                .setDomainName(domain)
                .setRegion(region)
                .setCity(city)
                .setCampus(campus)
//            .setRoom(room)
                .setCode(code)
                .setFriendlyName(friendlyName)
                .setGeolocation(site.getSite().getGeolocation())
                .setImplementState(ImplementState.Allocate)
                .setSiteType(site.getSite().getSiteType() != null ? site.getSite().getSiteType()
                        : SiteType.SITE)
                .setProperties(site.getSite().getProperties())
                .setSupportingRack(new LinkedList<>())
                .setAlarmState(AlarmSeverity.Unknown)
                .setOperationalState(OperStatus.Unknown);

        siteBuilder.setIsVirtual(siteBuilder.getSiteType() != SiteType.SITE);

        NodeBuilder nodeBuilder = new NodeBuilder()
                .setKey(new NodeKey(new NodeId(nodeId)))
                .setNodeId(new NodeId(nodeId))
                .setSupportingNode(new LinkedList<>())
                .setTerminationPoint(new LinkedList<>())
                .addAugmentation(Node1.class,
                        new Node1Builder().setSite(siteBuilder.build()).build());
        return nodeBuilder.build();
    }

    public SiteNodeCreator setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//        this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
