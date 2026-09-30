package net.flex.dci.otn.controller.allocate.network;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.common.Utils;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.network.bytedance.ase.ByteDanceSpec;
import net.flex.dci.otn.controller.allocate.node.view.graph.GraphHelp;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.Roadms;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.WssLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelationKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j
public class SummaryNetwork implements FutureCallback<List<CreationResult>> {

    //    private TaskInfoKafkaService kafka;
    private ZkResourceLock locker;


    private ChangedObject changedObject;
    private TaskInfoMessage taskInfoMessage;
    private List<String> siteLinkNameList;
    private List<WssLinks> wssLinks;
    private List<Roadms> roadmSiteInfo;

    public SummaryNetwork(ZkResourceLock locker, ChangedObject changedObject,
            TaskInfoMessage taskInfoMessage, List<String> siteLinkFriendlyNameList,
            List<WssLinks> wssLinks, List<Roadms> roadmSiteInfo) {
        this.changedObject = changedObject;
        this.taskInfoMessage = taskInfoMessage;
        this.siteLinkNameList = siteLinkFriendlyNameList;
        this.wssLinks = wssLinks;
        this.roadmSiteInfo = roadmSiteInfo;

        this.locker = locker;
//        this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);
    }

    private static String cnToUnicode(String cn) {
        char[] chars = cn.toCharArray();
        String returnStr = "";
        for (int i = 0; i < chars.length; i++) {
            String hex = Integer.toString(chars[i], 16);
            if (hex.length() <= 2) {
                //英文字符
                returnStr += chars[i];
            } else {
                returnStr += "\\u" + hex;
            }
        }
        return returnStr;
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("create network fail.", throwable);
        locker.unlock();
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        logMessage(throwable.getMessage());
    }

    @Override
    public void onSuccess(@Nullable List<CreationResult> siteLinkCreationResultList) {
        //siteLinkCreationResult is based on the format "siteLinkName, and success/fail";
        //check all success and then save siteLink, and related WSS Link to DB

        taskInfoMessage.setEndTime(System.currentTimeMillis());
        String errInfo = hasError(siteLinkCreationResultList);
        if (errInfo != null) {
            log.info("create network fail {}", errInfo);
            locker.unlock();

            logMessage(errInfo.toString());
            return;
        }

        log.info("create network success.");

        Map<String, SiteLinkKeyInfo> friendLyNameMap = new HashMap<>();  //key is UI provided, value is real Name
        for (CreationResult result : siteLinkCreationResultList) {
            Link siteLink = result.getCreatedLink();

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                    siteLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            .getSite();

            addNetworkIdentify(result.getCreatedLink(), taskInfoMessage.getResourceId());
            friendLyNameMap.put(result.getLinkName(),
                    new SiteLinkKeyInfo(result.getCreatedLink().getLinkId().getValue(),
                            siteLinkAttr.getFriendlyName()));

            //task中保存的信息是UI提供的，siteLink创建后具体的名称变了，所以task中的信息也要变
            String name = result.getLinkName().replace("(", "\\(")
                    .replace(")", "\\)");  //replaceAll's bug
            taskInfoMessage.setDetail(
                    taskInfoMessage.getDetail().replaceAll(name, siteLinkAttr.getFriendlyName()));
        }
        //save all data in DB
        try {
            //fetch wssLink (a phyLink is binding two siteLink.
            for (WssLinks wssLink : wssLinks) {
                log.debug("create wssLink : {}, {}", wssLink.getLinkId(), wssLink.getPhysical().getFriendlyName());

                createWssLink(wssLink, friendLyNameMap);
            }

            //update siteNode, recorde/update the siteLink is related to how many siteLink with wss
            for (Roadms roadmInfo : roadmSiteInfo) {
                Node siteNode = changedObject.getChangedSiteNode(roadmInfo.getSiteId());
                log.debug("update roadm info on siteNode {}, {}",
                        siteNode.getNodeId().getValue(),
                        siteNode.getAugmentation(Node1.class).getSite().getFriendlyName());

                updateRoadmInfo(friendLyNameMap, siteNode, roadmInfo.getSiteLinkRelation());
            }

            if (siteLinkCreationResultList.size() > 3) {
                new GraphHelp(changedObject).layout(changedObject.getChangedSiteNodeList(), changedObject.getChangedSiteLinkList());
            }
            Utils.store2DB(changedObject);

            updateDefaultParams(); // 这个用到共有库，里面需要基于siteLink 读取数据库phyLink， 所以只能先存盘

            logMessage(null);

            for (CreationResult result : siteLinkCreationResultList) {
                SiteLinkKeyInfo siteLinkInfo = friendLyNameMap.get(result.getLinkName());
                TaskInfoMessage siteLinkTaskInfo = new TaskInfoMessage(siteLinkInfo.getLinkId(),
                        taskInfoMessage.getWho(), TaskInfoMessage.ResourceType.siteLink,
                        TaskInfoMessage.ActionType.create);
                siteLinkTaskInfo.setResourceName(siteLinkInfo.friendlyName);
                siteLinkTaskInfo.setActionTime(taskInfoMessage.getActionTime());
                siteLinkTaskInfo.setEndTime(taskInfoMessage.getEndTime());
                siteLinkTaskInfo.setGroupId(taskInfoMessage.getGroupId());
                siteLinkTaskInfo.setRoot(false);
                logSiteLinkMessage(siteLinkTaskInfo);
            }

        } catch (Exception e) {
            log.error("error happen when creation network info", e);
            logMessage(e.getMessage());
        } finally {
            locker.unlock();
        }
    }

    private void updateDefaultParams() {
        log.debug("start setting Optical default params");
        AllocatorConfig allocateConfig = SpringBeanFinder.getBean(AllocatorConfig.class);
        Set<String> createdSiteLinkIds = changedObject.getChangedSiteLinkList().keySet();

        changedObject = new ChangedObject();
        try {
            createdSiteLinkIds.forEach(siteLinkId -> {
                if (isByteDanceFamily(allocateConfig.getYangModel())) {
                    new ByteDanceSpec(siteLinkId, changedObject).start();  //修改默认参数
                }
            });
        } catch (Exception e) {
            log.error("Setting default param of site link", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Setting default param of site link");
        }
        Utils.store2DB(changedObject);
    }

    private boolean isByteDanceFamily(NeYangModel yangModel) {
        // Chassis20 reuses the ByteDance default optical-parameter calculation.
        return NeYangModel.ByteDance.equals(yangModel) || NeYangModel.Chassis20.equals(yangModel);
    }

    private void addNetworkIdentify(Link createdLink, String networkId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                createdLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                        .getSite();
        Link newLink = new LinkBuilder(createdLink)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                .setSite(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder(
                                                siteLinkAttr)
                                                .setNetworkId(networkId)
                                                .setInvolvedNetwork(true)
                                                .build())
                                .build())
                .build();
        changedObject.addChangedSiteLink(newLink);
    }

    private void updateRoadmInfo(Map<String, SiteLinkKeyInfo> friendlyNameMap, Node siteNode,
            List<SiteLinkRelation> siteLinkRelationList) {
        //siteLinkRelation is coming from UI, thus it provided friendlyName is friendlyName's key
        Site siteNodeAttr = siteNode.getAugmentation(Node1.class).getSite();
        if (siteNodeAttr.getSiteType().equals(SiteType.OTM)) {
            //do not change type
        } else {
            //update type to roadm
            siteNodeAttr = new SiteBuilder(siteNodeAttr).setSiteType(SiteType.ROADM).build();
        }

        Integer index = 0;
        List<SiteLinkRelation> newRelationList;
        if (siteNodeAttr.getSiteLinkRelation() != null) {
            for (SiteLinkRelation relation : siteNodeAttr.getSiteLinkRelation()) {
                if (relation.getIndex() > index) {
                    index = relation.getIndex();
                }
            }
            newRelationList = siteNodeAttr.getSiteLinkRelation();
        } else {
            newRelationList = new ArrayList<>();
        }
        for (SiteLinkRelation uiProvidedRelation : siteLinkRelationList) {
            Link wssLink = changedObject.getChangedPhyLink(
                    uiProvidedRelation.getWssLinkIdBetweenAZ());
            String wssLinkFriendlyName = wssLink.getAugmentation(Link1.class).getPhysical()
                    .getFriendlyName();
            newRelationList.add(
                    new SiteLinkRelationBuilder().setIndex(index)
                            .setKey(new SiteLinkRelationKey(index))
                            .setLinka(friendlyNameMap.get(uiProvidedRelation.getLinka())
                                    .getFriendlyName())
                            .setLinkz(friendlyNameMap.get(uiProvidedRelation.getLinkz())
                                    .getFriendlyName())
                            .setLinkaId(
                                    friendlyNameMap.get(uiProvidedRelation.getLinka()).getLinkId())
                            .setLinkzId(
                                    friendlyNameMap.get(uiProvidedRelation.getLinkz()).getLinkId())
                            .setWssLinkBetweenAZ(wssLinkFriendlyName)
                            .setWssLinkIdBetweenAZ(wssLink.getLinkId().getValue())
                            .build()
            );
            index++;
        }

        Node newNode = new NodeBuilder(siteNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setSite(new SiteBuilder(siteNodeAttr)
                                .setSiteLinkRelation(newRelationList)
                                .build())
                        .build())
                .build();

        changedObject.addChangedSiteNode(newNode);
    }

    /**
     * WSS link original supported link provided from UI, theire friendlyName from UI, should
     * replace with created siteLinkID
     *
     * @param wssLink
     * @param friendLyNameMap <linkName(UI provided), <siteLinkFriendlyName, siteLinkId>>
     */
    private void createWssLink(WssLinks wssLink, Map<String, SiteLinkKeyInfo> friendLyNameMap) {
        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

        log.debug("create wssLink {}", wssLink);
        List<SupportedLink> supportedLinkList = new ArrayList<>();
        String planeId = null;
        String planeName = null;

        //wssLink 只会关联两个复用段， 所以wssLink.getPhysical().getSupportedLink()的size=2
        for (SupportedLink sl : wssLink.getPhysical().getSupportedLink()) {
            LinkId linkId;
            Link siteLink;
            if (! friendLyNameMap.containsKey(sl.getLinkRef().getValue())) {
                //这个supportingLink是已经存在的复用段，所以没有创建后的friendlyNameMapping
                siteLink = siteLinkDao.getLinkByFriendlyName(sl.getLinkRef().getValue());
                linkId = siteLink.getLinkId();
                friendLyNameMap.put(sl.getLinkRef().getValue(),
                        new SiteLinkKeyInfo(linkId.getValue(), sl.getLinkRef().getValue()));
            } else {
                linkId = new LinkId(friendLyNameMap.get(sl.getLinkRef().getValue()).getLinkId());
                siteLink = changedObject.getChangedSiteLink(linkId.getValue());
            }
            supportedLinkList.add(new SupportedLinkBuilder()
                    .setTopologyRef(sl.getTopologyRef())
                    .setLinkRef(linkId)
                    .setKey(new SupportedLinkKey(linkId, sl.getTopologyRef()))
                    .build());

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                    siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
            if (planeId != null && planeId.equals(siteLinkAttr.getPlaneId())) {
                //do nothing
            } else if (planeId == null) {
                planeId = siteLinkAttr.getPlaneId();
                planeName = siteLinkAttr.getPlaneName();
            } else {
                log.debug( "The planeId of two siteLinks connected by wssLink is different, please check.");
                planeId = null;
                planeName = null;
            }
        }
        Link phyLink = new LinkBuilder(wssLink).addAugmentation(Link1.class,
                        new Link1Builder().setPhysical(new PhysicalBuilder(wssLink.getPhysical())
                                        .setSupportedLink(supportedLinkList)
                                        .setPlaneId(planeId)
                                        .setPlaneName(planeName)
                                        .build())
                                .build())
                .build();

        updateFriendlyName(phyLink);
        PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
        phyLinkUtil.addPhyLinkAddtional(changedObject, phyLink);  // (internalInk, tp busy status)
    }

    private void updateFriendlyName(Link phyLink) {
        PhyLinkFriendlyName phyLinkFriendlyNameGenerator = SpringBeanFinder.getBean(
                PhyLinkFriendlyName.class);

        Node srcNode = changedObject.getChangedPhyNode(
                phyLink.getSource().getSourceNode().getValue());
        Node dstNode = changedObject.getChangedPhyNode(
                phyLink.getDestination().getDestNode().getValue());
        Link newLink = phyLinkFriendlyNameGenerator.updateFriendlyName(phyLink, srcNode, dstNode);

        changedObject.addChangedPhyLink(newLink);
    }

    private String hasError(List<CreationResult> siteLinkCreationResultList) {
        for (CreationResult result : siteLinkCreationResultList) {
            if (result.getErrorInfo() != null) {
                //error happen
                return result.getErrorInfo();
            }
        }
        return null;
    }


    private void  logMessage(String errorMessage) {
        String msg = String.format("Create %s ", taskInfoMessage.getResourceName());

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }

        taskInfoMessage.setSuccessfully(isOk);
        if (taskInfoMessage != null) {
            if (isOk) {
                //do nothing
            } else {
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("create network")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }


    private void logSiteLinkMessage(TaskInfoMessage siteLinkTaskInfo) {
        String msg = String.format("Create %s ", siteLinkTaskInfo.getResourceName());
        siteLinkTaskInfo.setSuccessfully(true);
        siteLinkTaskInfo.setDetail(null);

        TaskInfoMessager.sendMessage(siteLinkTaskInfo);

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(BroadCastConstant.CREATE_SITE_LINK)
                        .message(msg + "successfully.")
                        .error(false)
                        .build());
    }

    private class SiteLinkKeyInfo {

        /**
         * the linkId of created siteLink
         */
        String linkId;

        /**
         * the friendlyName of created siteLink
         */
        String friendlyName;

        public SiteLinkKeyInfo(String linkId, String friendlyName) {
            this.linkId = linkId;
            this.friendlyName = friendlyName;
        }

        public String getLinkId() {
            return linkId;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }
}
